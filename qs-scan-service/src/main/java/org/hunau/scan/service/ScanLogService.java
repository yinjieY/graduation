package org.hunau.scan.service;

import org.hunau.common.R;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.GeoUtil;
import org.hunau.common.util.MaskUtil;
import org.hunau.common.util.Sm2Util;
import org.hunau.scan.model.ScanLog;
import org.hunau.scan.model.ScanRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanLogService {

    private final RestTemplate restTemplate;
    private final List<ScanLog> logs = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> scannedQsSet = ConcurrentHashMap.newKeySet();

    @Value("${app.integration.alert-evaluate-url:http://localhost:8083/alert/evaluate}")
    private String alertEvaluateUrl;

    public ScanLogService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public R<?> handleScan(ScanRequest req) {
        AssertUtil.notNull(req, "扫码请求不能为空");
        AssertUtil.notEmpty(req.getQsId(), "qsId不能为空");
        AssertUtil.notEmpty(req.getSignaturePayload(), "签名载荷不能为空");
        AssertUtil.notEmpty(req.getSignature(), "签名不能为空");

        boolean valid = Sm2Util.verify(req.getSignaturePayload(), req.getSignature(), "TRACE_PRIVATE_KEY");
        if (!valid) {
            return R.fail("二维码验签失败");
        }

        ScanLog log = new ScanLog();
        log.setQsId(req.getQsId());
        log.setBatchId(req.getBatchId());
        log.setCompanyId(req.getCompanyId());
        log.setScanTime(LocalDateTime.now());
        log.setMaskedPhone(MaskUtil.maskPhone(req.getUserPhone()));
        log.setMaskedIp(MaskUtil.maskIp(req.getIp()));
        log.setDeviceFingerprint(req.getDeviceFingerprint());
        log.setOs(req.getOs());
        log.setBrowser(req.getBrowser());
        log.setLatitude(req.getLatitude());
        log.setLongitude(req.getLongitude());
        log.setFirstScan(scannedQsSet.add(req.getQsId()));

        boolean crossRegion = false;
        if (req.getLatitude() != null && req.getLongitude() != null
                && req.getExpectedLatitude() != null && req.getExpectedLongitude() != null) {
            crossRegion = GeoUtil.isCrossRegionRisk(req.getLatitude(), req.getLongitude(),
                    req.getExpectedLatitude(), req.getExpectedLongitude(), 100.0);
        }
        log.setCrossRegionRisk(crossRegion);
        logs.add(log);

        pushAlertEvaluate(log);
        return R.ok(log);
    }

    public R<?> listByQsId(String qsId) {
        List<ScanLog> result = new ArrayList<>();
        for (ScanLog log : logs) {
            if (qsId.equals(log.getQsId())) {
                result.add(log);
            }
        }
        return R.ok(result);
    }

    private void pushAlertEvaluate(ScanLog log) {
        Map<String, Object> body = new HashMap<>();
        body.put("qsId", log.getQsId());
        body.put("companyId", log.getCompanyId());
        body.put("scanCount", countRecentScans(log.getQsId(), 60));
        body.put("deviceCount", countRecentDevice(log.getQsId(), 60));
        body.put("ipCount", countRecentIp(log.getQsId(), 60));
        body.put("locationVariance", log.isCrossRegionRisk() ? 1.0 : 0.2);
        body.put("timeVariance", 0.5);
        try {
            restTemplate.postForObject(alertEvaluateUrl, body, R.class);
        } catch (Exception ignored) {
            // Alert service is eventually consistent; scan flow should not fail.
        }
    }

    private int countRecentScans(String qsId, int minutes) {
        LocalDateTime begin = LocalDateTime.now().minusMinutes(minutes);
        int count = 0;
        for (ScanLog log : logs) {
            if (qsId.equals(log.getQsId()) && log.getScanTime().isAfter(begin)) {
                count++;
            }
        }
        return count;
    }

    private int countRecentDevice(String qsId, int minutes) {
        LocalDateTime begin = LocalDateTime.now().minusMinutes(minutes);
        Set<String> devices = new HashSet<>();
        for (ScanLog log : logs) {
            if (qsId.equals(log.getQsId()) && log.getScanTime().isAfter(begin) && log.getDeviceFingerprint() != null) {
                devices.add(log.getDeviceFingerprint());
            }
        }
        return devices.size();
    }

    private int countRecentIp(String qsId, int minutes) {
        LocalDateTime begin = LocalDateTime.now().minusMinutes(minutes);
        Set<String> ips = new HashSet<>();
        for (ScanLog log : logs) {
            if (qsId.equals(log.getQsId()) && log.getScanTime().isAfter(begin) && log.getMaskedIp() != null) {
                ips.add(log.getMaskedIp());
            }
        }
        return ips.size();
    }
}

