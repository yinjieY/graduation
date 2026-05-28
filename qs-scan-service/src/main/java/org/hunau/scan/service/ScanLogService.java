package org.hunau.scan.service;

import org.hunau.common.model.R;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.GeoUtil;
import org.hunau.common.util.MaskUtil;
import org.hunau.common.util.Sm2Util;
import org.hunau.scan.client.AlertFeignClient;
import org.hunau.scan.client.TraceFeignClient;
import org.hunau.scan.model.DeviceProfile;
import org.hunau.scan.model.ScanLog;
import org.hunau.scan.model.ScanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScanLogService {

    private static final Logger log = LoggerFactory.getLogger(ScanLogService.class);
    private static final double CROSS_REGION_THRESHOLD_KM = 600.0;

    @Value("${app.crypto.sm2.public-key:TRACE_PRIVATE_KEY}")
    private String sm2PublicKey;

    private final AlertFeignClient alertFeignClient;
    private final TraceFeignClient traceFeignClient;
    private final JdbcTemplate jdbcTemplate;

    public ScanLogService(AlertFeignClient alertFeignClient, TraceFeignClient traceFeignClient, JdbcTemplate jdbcTemplate) {
        this.alertFeignClient = alertFeignClient;
        this.traceFeignClient = traceFeignClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public R<?> handleScan(ScanRequest req) {
        AssertUtil.notNull(req, "扫码请求不能为空");
        AssertUtil.notEmpty(req.getQsId(), "qsId不能为空");
        AssertUtil.notEmpty(req.getSignaturePayload(), "签名载荷不能为空");
        AssertUtil.notEmpty(req.getSignature(), "签名不能为空");

        Map<String, Object> qrInfo = validateQrAvailable(req);
        String qrAvailableMsg = (String) qrInfo.get("error");
        if (qrAvailableMsg != null) {
            return R.fail(qrAvailableMsg);
        }
        Integer maxAllowedScans = (Integer) qrInfo.get("maxAllowedScans");

        String payloadCheckMsg = checkPayloadConsistency(req);
        if (payloadCheckMsg != null) {
            return R.fail(payloadCheckMsg);
        }

        boolean valid = Sm2Util.verify(req.getSignaturePayload(), req.getSignature(), sm2PublicKey);
        if (!valid) {
            return R.fail("ScanLogService#handleScan 验签失败：signature 与 payload 或 app.crypto.sm2.public-key 不匹配");
        }

        ScanLog log = new ScanLog();
        log.setQsId(req.getQsId());
        log.setBatchId(req.getBatchId());
        log.setCompanyId(req.getCompanyId());
        log.setScanTime(LocalDateTime.now());
        log.setMaskedIp(MaskUtil.maskIp(req.getIp()));
        log.setDeviceFingerprint(req.getDeviceFingerprint());
        log.setBrowser(req.getBrowser());
        log.setLatitude(req.getLatitude());
        log.setLongitude(req.getLongitude());
        log.setLocationSource(normalize(req.getLocationSource()));
        log.setFirstScan(countTotalScans(req.getQsId()) == 0);

        double distanceKm = calcDistanceKm(req);
        log.setDistanceKm(distanceKm);
        log.setCrossRegionRisk(distanceKm > CROSS_REGION_THRESHOLD_KM);

        DeviceProfile profile = updateDeviceProfile(req, log.getScanTime());
        log.setNewDevice(profile.getScanCount() == 1);
        log.setRiskDevice(profile.isRisk());

        persistScanLog(log, req);
        
        Map<String, Object> result = new HashMap<>();
        result.put("scanLog", log);
        
        try {
            Map<String, Object> evaluationResult = pushAlertEvaluate(log, profile, maxAllowedScans);
            if (evaluationResult != null) {
                result.put("riskEvaluation", evaluationResult);
                ScanLogService.log.info("[扫描上报] 风险评估成功: riskLevel={}, riskScore={}", 
                        evaluationResult.get("riskLevel"), evaluationResult.get("riskScore"));
            } else {
                ScanLogService.log.warn("[扫描上报] 风险评估返回空结果");
            }
        } catch (Exception e) {
            ScanLogService.log.warn("[扫描上报] 风险评估异常: {}", e.getMessage());
        }
        
        return R.ok(result);
    }

    public R<?> listByQsId(String qsId) {
        List<ScanLog> result = jdbcTemplate.query(
                "SELECT s.qs_id,s.batch_id,s.company_id,s.scan_time,s.ip_masked,s.device_fingerprint,s.browser," +
                        "s.lat,s.lng,s.is_first,s.location_source,s.distance_km,s.new_device,s.risk_device," +
                        "dp.city,dp.province " +
                        "FROM yx_scan_anomaly.scan_log s " +
                        "LEFT JOIN yx_geo_profile.device_profile dp ON s.device_fingerprint = dp.device_fingerprint " +
                        "WHERE s.qs_id = ? ORDER BY s.scan_time DESC",
                scanLogRowMapper(),
                qsId);
        return R.ok(result);
    }

    public R<?> getReuseFeatureByQsId(String qsId) {
        AssertUtil.notEmpty(qsId, "qsId不能为空");
        Map<String, Object> feature = loadReuseFeature(qsId);
        if (feature == null) {
            return R.fail("未找到该二维码的复用特征，请先触发聚合");
        }
        return R.ok(feature);
    }

    private DeviceProfile updateDeviceProfile(ScanRequest req, LocalDateTime now) {
        String fingerprint = normalize(req.getDeviceFingerprint());
        if (fingerprint == null) {
            fingerprint = "unknown-device";
        }
        DeviceProfile profile = findDeviceProfile(fingerprint);
        if (profile == null) {
            profile = new DeviceProfile();
            profile.setDeviceFingerprint(fingerprint);
            profile.setFirstSeen(now);
            profile.setScanCount(0);
        }

        profile.setLastSeen(now);
        profile.setScanCount(profile.getScanCount() + 1);
        profile.setBrowser(normalize(req.getBrowser()));
        boolean highFrequency = profile.getScanCount() >= 20;
        profile.setRisk(profile.isRisk() || highFrequency);

        upsertDeviceProfile(profile);
        return profile;
    }

    private Map<String, Object> pushAlertEvaluate(ScanLog log, DeviceProfile profile, Integer maxAllowedScans) {
        Map<String, Object> reuseFeature = loadReuseFeature(log.getQsId());
        int scanCount1h = countRecentScans(log.getQsId(), 60);
        int deviceCount1d = countRecentDevice(log.getQsId(), 1440);
        int ipCount1h = countRecentIp(log.getQsId(), 60);

        int scanCount = reuseFeature == null ? scanCount1h : intValue(reuseFeature.get("scan_count"), scanCount1h);
        int deviceCount = reuseFeature == null ? deviceCount1d : intValue(reuseFeature.get("device_count"), deviceCount1d);
        int ipCount = reuseFeature == null ? ipCount1h : intValue(reuseFeature.get("ip_count"), ipCount1h);
        double baseLocVar = log.isCrossRegionRisk() ? 1.0 : 0.04;
        double locationVariance = reuseFeature == null
                ? baseLocVar
                : doubleValue(reuseFeature.get("location_variance"), baseLocVar);
        double timeVariance = reuseFeature == null ? 0.04 : doubleValue(reuseFeature.get("time_variance"), 0.0);

        Map<String, Object> body = new HashMap<>();
        body.put("qsId", log.getQsId());
        body.put("companyId", log.getCompanyId());
        body.put("scanCount1h", scanCount1h);
        body.put("deviceCount1d", deviceCount1d);
        body.put("ipCount1h", ipCount1h);

        body.put("scanCount", scanCount);
        body.put("deviceCount", deviceCount);
        body.put("ipCount", ipCount);
        body.put("locationVariance", locationVariance);
        body.put("timeVariance", timeVariance);
        body.put("newDevice", log.isNewDevice());
        body.put("riskDevice", log.isRiskDevice());
        body.put("distanceKm", log.getDistanceKm());
        body.put("deviceScanCount", profile.getScanCount());
        body.put("maxAllowedScans", maxAllowedScans);
        body.put("city", null);
        body.put("province", null);
        
        try {
            R<?> response = alertFeignClient.evaluate(body);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getData();
                ScanLogService.log.info("[扫描上报] 风险评估结果: riskLevel={}, riskScore={}", 
                        result.get("riskLevel"), result.get("riskScore"));
                return result;
            }
        } catch (Exception e) {
            ScanLogService.log.warn("[扫描上报] 风险评估调用失败: {}", e.getMessage());
        }
        return null;
    }

    private double calcDistanceKm(ScanRequest req) {
        if (req.getLatitude() == null || req.getLongitude() == null) {
            log.info("[跨区域检测] 扫码位置为空(lat={}, lng={})，跳过距离计算", req.getLatitude(), req.getLongitude());
            return 0.0;
        }

        Double expectedLat = req.getExpectedLatitude();
        Double expectedLng = req.getExpectedLongitude();

        log.info("[跨区域检测] 阶段1/3 - 扫码位置: lat={}, lng={}", req.getLatitude(), req.getLongitude());

        if (expectedLat == null || expectedLng == null) {
            Map<String, Double> location = queryCompanyLocation(req.getCompanyId());
            expectedLat = location.get("lat");
            expectedLng = location.get("lng");
            log.info("[跨区域检测] 阶段2/3 - 从数据库查询企业位置: companyId={}, lat={}, lng={}", req.getCompanyId(), expectedLat, expectedLng);
        } else {
            log.info("[跨区域检测] 阶段2/3 - 使用请求中的预期位置: lat={}, lng={}", expectedLat, expectedLng);
        }

        if (expectedLat == null || expectedLng == null) {
            log.warn("[跨区域检测] 阶段2/3 - 企业位置为空，无法计算距离");
            return 0.0;
        }

        double distance = GeoUtil.distanceKm(req.getLatitude(), req.getLongitude(), expectedLat, expectedLng);
        boolean isCrossRegion = distance > CROSS_REGION_THRESHOLD_KM;
        log.info("[跨区域检测] 阶段3/3 - 计算完成: 距离={}km, 阈值={}km, 跨区域风险={}", distance, CROSS_REGION_THRESHOLD_KM, isCrossRegion);

        return distance;
    }

    private Map<String, Double> queryCompanyLocation(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            return Map.of("lat", null, "lng", null);
        }
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    "SELECT lat, lng FROM yx_trace_core.company WHERE company_id = ?",
                    companyId
            );
            return Map.of(
                    "lat", result.get("lat") != null ? ((Number) result.get("lat")).doubleValue() : null,
                    "lng", result.get("lng") != null ? ((Number) result.get("lng")).doubleValue() : null
            );
        } catch (Exception e) {
            return Map.of("lat", null, "lng", null);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String checkPayloadConsistency(ScanRequest req) {
        String[] parts = req.getSignaturePayload().split("\\|", -1);
        if (parts.length < 4) {
            return "二维码载荷格式错误";
        }
        if (!Objects.equals(parts[0], req.getQsId())) {
            return "二维码校验失败：qsId不一致";
        }
        if (req.getBatchId() != null && !req.getBatchId().isBlank() && !Objects.equals(parts[1], req.getBatchId())) {
            return "二维码校验失败：batchId不一致";
        }
        if (req.getCompanyId() != null && !req.getCompanyId().isBlank() && !Objects.equals(parts[2], req.getCompanyId())) {
            return "二维码校验失败：companyId不一致";
        }
        return null;
    }

    private int countRecentScans(String qsId, int windowMinutes) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM yx_scan_anomaly.scan_log WHERE qs_id = ? AND scan_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)",
                Integer.class,
                qsId,
                windowMinutes);
        return count == null ? 0 : count;
    }

    private int countRecentDevice(String qsId, int windowMinutes) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT device_fingerprint) FROM yx_scan_anomaly.scan_log WHERE qs_id = ? AND scan_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)",
                Integer.class,
                qsId,
                windowMinutes);
        return count == null ? 0 : count;
    }

    private int countRecentIp(String qsId, int windowMinutes) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT ip_masked) FROM yx_scan_anomaly.scan_log WHERE qs_id = ? AND scan_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)",
                Integer.class,
                qsId,
                windowMinutes);
        return count == null ? 0 : count;
    }

    private int countTotalScans(String qsId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM yx_scan_anomaly.scan_log WHERE qs_id = ?",
                Integer.class,
                qsId);
        return count == null ? 0 : count;
    }

    private DeviceProfile findDeviceProfile(String fingerprint) {
        List<DeviceProfile> rows = jdbcTemplate.query(
                "SELECT device_fingerprint,first_seen,last_seen,city,province,os,browser,is_risk,scan_count FROM yx_geo_profile.device_profile WHERE device_fingerprint = ?",
                (rs, rowNum) -> {
                    DeviceProfile profile = new DeviceProfile();
                    profile.setDeviceFingerprint(rs.getString("device_fingerprint"));
                    profile.setFirstSeen(toLocalDateTime(rs.getTimestamp("first_seen")));
                    profile.setLastSeen(toLocalDateTime(rs.getTimestamp("last_seen")));
                    profile.setCity(rs.getString("city"));
                    profile.setProvince(rs.getString("province"));
                    profile.setOs(rs.getString("os"));
                    profile.setBrowser(rs.getString("browser"));
                    profile.setRisk(rs.getBoolean("is_risk"));
                    profile.setScanCount(rs.getInt("scan_count"));
                    return profile;
                },
                fingerprint);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void upsertDeviceProfile(DeviceProfile profile) {
        jdbcTemplate.update(
                "INSERT INTO yx_geo_profile.device_profile (device_fingerprint,first_seen,last_seen,city,province,os,browser,is_risk,scan_count) " +
                        "VALUES (?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE last_seen=VALUES(last_seen), city=VALUES(city), province=VALUES(province), os=VALUES(os), browser=VALUES(browser), is_risk=VALUES(is_risk), scan_count=VALUES(scan_count)",
                profile.getDeviceFingerprint(),
                profile.getFirstSeen(),
                profile.getLastSeen(),
                Optional.ofNullable(profile.getCity()).orElse("未知城市"),
                Optional.ofNullable(profile.getProvince()).orElse("未知省份"),
                Optional.ofNullable(profile.getOs()).orElse("Other"),
                Optional.ofNullable(profile.getBrowser()).orElse("Browser"),
                profile.isRisk() ? 1 : 0,
                profile.getScanCount());
    }

    private void persistScanLog(ScanLog log, ScanRequest req) {
        jdbcTemplate.update(
                "INSERT INTO yx_scan_anomaly.scan_log (qs_id,batch_id,company_id,scan_time,ip,ip_masked,device_fingerprint,browser,lat,lng,is_first,location_source,distance_km,new_device,risk_device) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                log.getQsId(),
                log.getBatchId(),
                log.getCompanyId(),
                log.getScanTime(),
                Optional.ofNullable(normalize(req.getIp())).orElse("0.0.0.0"),
                Optional.ofNullable(log.getMaskedIp()).orElse("0.0.0.*"),
                Optional.ofNullable(log.getDeviceFingerprint()).orElse("unknown-device"),
                Optional.ofNullable(log.getBrowser()).orElse("Browser"),
                log.getLatitude(),
                log.getLongitude(),
                log.isFirstScan() ? 1 : 0,
                log.getLocationSource(),
                log.getDistanceKm(),
                log.isNewDevice() ? 1 : 0,
                log.isRiskDevice() ? 1 : 0);
    }

    private RowMapper<ScanLog> scanLogRowMapper() {
        return (rs, rowNum) -> {
            ScanLog log = new ScanLog();
            log.setQsId(rs.getString("qs_id"));
            log.setBatchId(rs.getString("batch_id"));
            log.setCompanyId(rs.getString("company_id"));
            log.setScanTime(toLocalDateTime(rs.getTimestamp("scan_time")));
            log.setMaskedIp(rs.getString("ip_masked"));
            log.setDeviceFingerprint(rs.getString("device_fingerprint"));
            log.setBrowser(rs.getString("browser"));
            log.setLatitude(rs.getObject("lat") == null ? null : rs.getDouble("lat"));
            log.setLongitude(rs.getObject("lng") == null ? null : rs.getDouble("lng"));
            log.setLocationSource(rs.getString("location_source"));
            log.setDistanceKm(rs.getObject("distance_km") == null ? null : rs.getDouble("distance_km"));
            log.setFirstScan(rs.getInt("is_first") == 1);
            log.setNewDevice(rs.getInt("new_device") == 1);
            log.setRiskDevice(rs.getInt("risk_device") == 1);
            log.setCity(rs.getString("city"));
            log.setProvince(rs.getString("province"));
            return log;
        };
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Map<String, Object> validateQrAvailable(ScanRequest req) {
        Map<String, Object> result = new HashMap<>();
        
        R<Map<String, Object>> resp;
        try {
            resp = traceFeignClient.queryTrace(req.getQsId());
        } catch (Exception ex) {
            result.put("error", "二维码状态校验失败：溯源服务调用异常");
            return result;
        }
        if (resp == null) {
            result.put("error", "二维码状态校验失败：溯源服务无响应");
            return result;
        }
        if (resp.getCode() != 200 || resp.getData() == null) {
            result.put("error", "二维码不可用：" + (resp.getMsg() == null ? "查询失败" : resp.getMsg()));
            return result;
        }

        Object qsCodeRaw = resp.getData().get("qsCode");
        if (!(qsCodeRaw instanceof Map<?, ?> qsCodeMap)) {
            result.put("error", "二维码状态校验失败：溯源返回格式异常");
            return result;
        }

        String status = stringValue(qsCodeMap.get("status"));
        if (!"active".equalsIgnoreCase(status)) {
            result.put("error", "二维码不可用：当前状态为 " + (status == null ? "unknown" : status));
            return result;
        }

        String batchId = stringValue(qsCodeMap.get("batchId"));
        String companyId = stringValue(qsCodeMap.get("companyId"));
        if ((req.getBatchId() == null || req.getBatchId().isBlank()) && batchId != null) {
            req.setBatchId(batchId);
        }
        if ((req.getCompanyId() == null || req.getCompanyId().isBlank()) && companyId != null) {
            req.setCompanyId(companyId);
        }

        Integer maxAllowedScans = intValue(qsCodeMap.get("maxAllowedScans"));
        result.put("maxAllowedScans", maxAllowedScans);
        
        if (maxAllowedScans != null && maxAllowedScans > 0) {
            int scanned = countTotalScans(req.getQsId());
            int freezeThreshold = maxAllowedScans * 3;
            if (scanned >= freezeThreshold) {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "frozen");
                    traceFeignClient.changeStatus(req.getQsId(), body);
                } catch (Exception ignored) {}
                result.put("error", "二维码不可用：扫码次数异常，已冻结(" + scanned + "/" + freezeThreshold + ")");
                return result;
            }
        }
        return result;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> loadReuseFeature(String qsId) {
        try {
            return jdbcTemplate.query(
                    "SELECT qs_id,scan_count,device_count,ip_count,time_variance,location_variance,update_time FROM yx_ai_feature.reuse_pattern WHERE qs_id=?",
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        Map<String, Object> data = new HashMap<>();
                        data.put("qsId", rs.getString("qs_id"));
                        data.put("scan_count", rs.getInt("scan_count"));
                        data.put("device_count", rs.getInt("device_count"));
                        data.put("ip_count", rs.getInt("ip_count"));
                        data.put("time_variance", rs.getDouble("time_variance"));
                        data.put("location_variance", rs.getDouble("location_variance"));
                        java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
                        data.put("update_time", updateTime == null ? null : updateTime.toLocalDateTime().toString());
                        return data;
                    },
                    qsId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
