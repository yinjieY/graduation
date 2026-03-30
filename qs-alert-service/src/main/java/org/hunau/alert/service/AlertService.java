package org.hunau.alert.service;

import org.hunau.alert.AiRiskService;
import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.AlertRecord;
import org.hunau.common.R;
import org.hunau.common.RiskLevel;
import org.hunau.common.util.AssertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    private final AiRiskService aiRiskService;
    private final RestTemplate restTemplate;
    private final List<AlertRecord> records = Collections.synchronizedList(new ArrayList<>());

    @Value("${app.integration.trace-freeze-url:http://localhost:8081/trace/qs/%s/status}")
    private String traceFreezeUrlPattern;

    @Value("${app.integration.block-event-url:http://localhost:8085/block/proof/event}")
    private String blockEventUrl;

    public AlertService(AiRiskService aiRiskService, RestTemplate restTemplate) {
        this.aiRiskService = aiRiskService;
        this.restTemplate = restTemplate;
    }

    public R<AlertRecord> evaluate(AlertEvaluateRequest req) {
        AssertUtil.notNull(req, "请求不能为空");
        AssertUtil.notEmpty(req.getQsId(), "qsId不能为空");
        AssertUtil.notEmpty(req.getCompanyId(), "companyId不能为空");

        int scanCount = Optional.ofNullable(req.getScanCount()).orElse(0);
        int deviceCount = Optional.ofNullable(req.getDeviceCount()).orElse(0);
        int ipCount = Optional.ofNullable(req.getIpCount()).orElse(0);
        double timeVar = Optional.ofNullable(req.getTimeVariance()).orElse(0.0);
        double locVar = Optional.ofNullable(req.getLocationVariance()).orElse(0.0);

        double ruleScore = ruleScore(scanCount, deviceCount, ipCount, locVar);
        float aiScore = aiRiskService.predict(scanCount, (float) timeVar, (float) locVar, deviceCount);
        double score = Math.min(1.0, Math.max(0.0, aiScore * 0.6 + ruleScore * 0.4));
        RiskLevel level = decideLevel(score);

        AlertRecord record = new AlertRecord();
        record.setEventId(UUID.randomUUID().toString().replace("-", ""));
        record.setQsId(req.getQsId());
        record.setCompanyId(req.getCompanyId());
        record.setRiskScore(score);
        record.setRiskLevel(level);
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus(level == RiskLevel.LOW ? "IGNORED" : "OPEN");
        record.setDetail("scan=" + scanCount + ", device=" + deviceCount + ", ip=" + ipCount + ", locVar=" + locVar);

        if (level != RiskLevel.LOW) {
            notifyRegulator(record);
            saveEventProof(record);
        }
        if (level == RiskLevel.HIGH) {
            freezeQsCode(record.getQsId());
        }

        records.add(record);
        return R.ok(record);
    }

    public R<List<AlertRecord>> list() {
        List<AlertRecord> result = new ArrayList<>(records);
        result.sort(Comparator.comparing(AlertRecord::getCreatedAt).reversed());
        return R.ok(result);
    }

    private double ruleScore(int scanCount, int deviceCount, int ipCount, double locVar) {
        double score = 0.0;
        if (scanCount >= 5) {
            score += 0.35;
        }
        if (deviceCount >= 10) {
            score += 0.30;
        }
        if (ipCount >= 20) {
            score += 0.25;
        }
        if (locVar >= 1.0) {
            score += 0.20;
        }
        return Math.min(1.0, score);
    }

    private RiskLevel decideLevel(double score) {
        if (score >= 0.75) {
            return RiskLevel.HIGH;
        }
        if (score >= 0.45) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private void notifyRegulator(AlertRecord record) {
        record.setNotifyChannel("SYSTEM,SMS,EMAIL");
        int retry = 0;
        boolean success = false;
        while (!success && retry <= 1) {
            retry++;
            try {
                // MVP only marks notification as successful.
                success = true;
            } catch (Exception ignored) {
                success = false;
            }
        }
        record.setNotifyRetry(retry - 1);
        if (!success) {
            record.setStatus("NOTIFY_FAILED");
        }
    }

    private void saveEventProof(AlertRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", record.getEventId());
        payload.put("qsId", record.getQsId());
        payload.put("riskLevel", record.getRiskLevel().name());
        payload.put("riskScore", record.getRiskScore());
        payload.put("action", "EVENT_RECORDED");
        tryPost(blockEventUrl, payload);
    }

    private void freezeQsCode(String qsId) {
        String url = String.format(traceFreezeUrlPattern, qsId);
        Map<String, Object> body = new HashMap<>();
        body.put("status", "frozen");
        tryPut(url, body);
    }

    private void tryPost(String url, Map<String, Object> body) {
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            try {
                restTemplate.postForObject(url, body, R.class);
                return;
            } catch (Exception ignored) {
                // Retry once then give up to keep alert flow non-blocking.
            }
        }
    }

    private void tryPut(String url, Map<String, Object> body) {
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            try {
                restTemplate.put(url, body);
                return;
            } catch (Exception ignored) {
                // Retry once then give up to keep alert flow non-blocking.
            }
        }
    }
}
