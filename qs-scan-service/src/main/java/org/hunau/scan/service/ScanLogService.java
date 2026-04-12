package org.hunau.scan.service;

import org.hunau.common.R;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.GeoUtil;
import org.hunau.common.util.MaskUtil;
import org.hunau.common.util.Sm2Util;
import org.hunau.scan.client.AlertFeignClient;
import org.hunau.scan.client.TraceFeignClient;
import org.hunau.scan.model.DeviceProfile;
import org.hunau.scan.model.ScanLog;
import org.hunau.scan.model.ScanRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScanLogService {

    private static final double CROSS_REGION_THRESHOLD_KM = 100.0;

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

        String qrAvailableMsg = validateQrAvailable(req);
        if (qrAvailableMsg != null) {
            return R.fail(qrAvailableMsg);
        }

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

        //日志存库
        persistScanLog(log, req);
        // 触发风险评估（调用AI模型）
        pushAlertEvaluate(log);
        return R.ok(log);
    }

    public R<?> listByQsId(String qsId) {
        List<ScanLog> result = jdbcTemplate.query(
                "SELECT qs_id,batch_id,company_id,scan_time,ip_masked,device_fingerprint,browser,lat,lng,is_first,location_source,distance_km,new_device,risk_device " +
                        "FROM yx_scan_anomaly.scan_log WHERE qs_id = ? ORDER BY scan_time DESC",
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

    private void pushAlertEvaluate(ScanLog log) {
        Map<String, Object> reuseFeature = loadReuseFeature(log.getQsId());
        int scanCount1h = countRecentScans(log.getQsId(), 60);
        int deviceCount1d = countRecentDevice(log.getQsId(), 1440);
        int ipCount1h = countRecentIp(log.getQsId(), 60);

        int scanCount = reuseFeature == null ? scanCount1h : intValue(reuseFeature.get("scan_count"), scanCount1h);
        int deviceCount = reuseFeature == null ? deviceCount1d : intValue(reuseFeature.get("device_count"), deviceCount1d);
        int ipCount = reuseFeature == null ? ipCount1h : intValue(reuseFeature.get("ip_count"), ipCount1h);
        double locationVariance = reuseFeature == null
                ? (log.isCrossRegionRisk() ? 1.0 : 0.2)
                : doubleValue(reuseFeature.get("location_variance"), 0.0);
        double timeVariance = reuseFeature == null ? 0.5 : doubleValue(reuseFeature.get("time_variance"), 0.0);

        Map<String, Object> body = new HashMap<>();
        body.put("qsId", log.getQsId());
        body.put("companyId", log.getCompanyId());
        body.put("scanCount1h", scanCount1h);
        body.put("deviceCount1d", deviceCount1d);
        body.put("ipCount1h", ipCount1h);

        // Backward-compatible fields retained for existing consumers.
        body.put("scanCount", scanCount);
        body.put("deviceCount", deviceCount);
        body.put("ipCount", ipCount);
        body.put("locationVariance", locationVariance);
        body.put("timeVariance", timeVariance);
        body.put("newDevice", log.isNewDevice());
        body.put("riskDevice", log.isRiskDevice());
        body.put("distanceKm", log.getDistanceKm());
        body.put("city", null);
        body.put("province", null);
        try {
            alertFeignClient.evaluate(body);
        } catch (Exception ignored) {
            // Alert service is eventually consistent; scan flow should not fail.
        }
    }

    private double calcDistanceKm(ScanRequest req) {
        if (req.getLatitude() == null || req.getLongitude() == null
                || req.getExpectedLatitude() == null || req.getExpectedLongitude() == null) {
            return 0.0;
        }
        return GeoUtil.distanceKm(req.getLatitude(), req.getLongitude(), req.getExpectedLatitude(), req.getExpectedLongitude());
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
            return log;
        };
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String validateQrAvailable(ScanRequest req) {
        R<Map<String, Object>> resp;
        try {
            resp = traceFeignClient.queryTrace(req.getQsId());
        } catch (Exception ex) {
            return "二维码状态校验失败：溯源服务调用异常";
        }
        if (resp == null) {
            return "二维码状态校验失败：溯源服务无响应";
        }
        if (resp.getCode() != 200 || resp.getData() == null) {
            return "二维码不可用：" + (resp.getMsg() == null ? "查询失败" : resp.getMsg());
        }

        Object qsCodeRaw = resp.getData().get("qsCode");
        if (!(qsCodeRaw instanceof Map<?, ?> qsCodeMap)) {
            return "二维码状态校验失败：溯源返回格式异常";
        }

        String status = stringValue(qsCodeMap.get("status"));
        if (!"active".equalsIgnoreCase(status)) {
            return "二维码不可用：当前状态为 " + (status == null ? "unknown" : status);
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
        if (maxAllowedScans != null && maxAllowedScans > 0) {
            int scanned = countTotalScans(req.getQsId());
            int freezeThreshold = maxAllowedScans * 3;
            if (scanned >= freezeThreshold) {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("status", "frozen");
                    traceFeignClient.changeStatus(req.getQsId(), body);
                } catch (Exception ignored) {}
                return "二维码不可用：扫码次数异常，已冻结(" + scanned + "/" + freezeThreshold + ")";
            }
        }
        return null;
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
