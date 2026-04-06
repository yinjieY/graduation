package org.hunau.alert.service;

import org.hunau.alert.AiRiskService;
import org.hunau.alert.client.BlockFeignClient;
import org.hunau.alert.client.TraceFeignClient;
import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.AlertRecord;
import org.hunau.alert.rule.RuleEngineResult;
import org.hunau.common.R;
import org.hunau.common.RiskLevel;
import org.hunau.common.util.AssertUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    private final AiRiskService aiRiskService;
    private final AlertRuleEngineService alertRuleEngineService;
    private final TraceFeignClient traceFeignClient;
    private final BlockFeignClient blockFeignClient;
    private final JdbcTemplate jdbcTemplate;

    public AlertService(AiRiskService aiRiskService,
                        AlertRuleEngineService alertRuleEngineService,
                        TraceFeignClient traceFeignClient,
                        BlockFeignClient blockFeignClient,
                        JdbcTemplate jdbcTemplate) {
        this.aiRiskService = aiRiskService;
        this.alertRuleEngineService = alertRuleEngineService;
        this.traceFeignClient = traceFeignClient;
        this.blockFeignClient = blockFeignClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public R<AlertRecord> evaluate(AlertEvaluateRequest req) {
        AssertUtil.notNull(req, "请求不能为空");
        AssertUtil.notEmpty(req.getQsId(), "qsId不能为空");
        AssertUtil.notEmpty(req.getCompanyId(), "companyId不能为空");

        int scanCount1h = firstNonNull(req.getScanCount1h(), req.getScanCount());
        int deviceCount1d = firstNonNull(req.getDeviceCount1d(), req.getDeviceCount());
        int ipCount1h = firstNonNull(req.getIpCount1h(), req.getIpCount());
        double timeVar = Optional.ofNullable(req.getTimeVariance()).orElse(0.0);
        double locVar = Optional.ofNullable(req.getLocationVariance()).orElse(0.0);
        boolean newDevice = Boolean.TRUE.equals(req.getNewDevice());
        boolean riskDevice = Boolean.TRUE.equals(req.getRiskDevice());
        double distanceKm = Optional.ofNullable(req.getDistanceKm()).orElse(0.0);

        RuleEngineResult ruleResult = alertRuleEngineService.evaluate(scanCount1h, deviceCount1d, ipCount1h);
        double ruleScore = enrichRuleScore(ruleResult.ruleScore(), locVar, newDevice, riskDevice, distanceKm);
        AiRiskService.PredictionResult aiResult = aiRiskService.predictDetailed(scanCount1h, (float) timeVar, (float) locVar, deviceCount1d);
        float aiScore = aiResult.score();
        double score = Math.min(1.0, Math.max(0.0, aiScore * 0.6 + ruleScore * 0.4));
        RiskLevel level = decideLevel(score);

        AlertRecord record = new AlertRecord();
        record.setEventId(UUID.randomUUID().toString().replace("-", ""));
        record.setQsId(req.getQsId());
        record.setCompanyId(req.getCompanyId());
        record.setRiskScore(score);
        record.setRiskLevel(level);
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus(level == RiskLevel.LOW ? "CLOSED" : "OPEN");
        record.setDetail("scan1h=" + scanCount1h
                + ", device1d=" + deviceCount1d
                + ", ip1h=" + ipCount1h
                + ", locVar=" + locVar
                + ", distanceKm=" + distanceKm
                + ", newDevice=" + newDevice
                + ", riskDevice=" + riskDevice
                + ", hitRules=" + ruleResult.hitRuleIds()
                + ", hitReasons=" + ruleResult.hitReasons()
                + ", aiMode=" + aiResult.mode()
                + ", aiRaw=" + aiResult.rawPreview()
                + ", aiScore=" + aiScore
                + ", ruleScore=" + ruleScore
                + ", province=" + req.getProvince()
                + ", city=" + req.getCity());

        persistAlertRecord(record, ruleResult, level);

        if (level != RiskLevel.LOW) {
            notifyRegulator(record);
            saveEventProof(record);
        }
        if (level == RiskLevel.HIGH) {
            freezeQsCode(record.getQsId());
        }

        return R.ok(record);
    }

    public R<List<AlertRecord>> list() {
        List<AlertRecord> result = jdbcTemplate.query(
                "SELECT alert_id,qs_id,company_id,alert_level,reason,detail,created_at,status FROM alert_record ORDER BY alert_id DESC LIMIT 500",
                (rs, rowNum) -> {
                    AlertRecord record = new AlertRecord();
                    record.setEventId(String.valueOf(rs.getLong("alert_id")));
                    record.setQsId(rs.getString("qs_id"));
                    record.setCompanyId(rs.getString("company_id"));
                    record.setRiskLevel(toRiskLevel(rs.getInt("alert_level")));
                    record.setDetail(rs.getString("detail"));
                    record.setStatus("open".equalsIgnoreCase(rs.getString("status")) ? "OPEN" : "CLOSED");
                    record.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
                    return record;
                }
        );
        return R.ok(result);
    }

    private double enrichRuleScore(double ruleScore,
                             double locVar,
                             boolean newDevice,
                             boolean riskDevice,
                             double distanceKm) {
        double score = ruleScore;
        if (locVar >= 1.0) {
            score += 0.20;
        }
        if (distanceKm >= 100.0) {
            score += 0.20;
        }
        if (newDevice) {
            score += 0.10;
        }
        if (riskDevice) {
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
        boolean success = sendNotification(record);
        record.setNotifyRetry(0);
        if (!success) {
            record.setStatus("NOTIFY_FAILED");
        }
        persistNotifyAction(record, success ? "发送预警成功" : "发送预警失败");
    }

    private void saveEventProof(AlertRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", record.getEventId());
        payload.put("qsId", record.getQsId());
        payload.put("riskLevel", record.getRiskLevel().name());
        payload.put("riskScore", record.getRiskScore());
        payload.put("action", "EVENT_RECORDED");
        try {
            blockFeignClient.saveEventProof(payload);
        } catch (Exception ignored) {
            // keep alert path non-blocking when block service is unstable
        }
    }

    private void freezeQsCode(String qsId) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "frozen");
        try {
            R<?> response = traceFeignClient.changeStatus(qsId, body);
            if (response != null && response.getCode() == 200) {
                persistActionByQsId(qsId, "冻结二维码成功");
                return;
            }
            String reason = response == null ? "响应为空" : String.valueOf(response.getMsg());
            persistActionByQsId(qsId, "冻结二维码失败: " + reason);
        } catch (Exception ex) {
            String reason = ex.getClass().getSimpleName() + (ex.getMessage() == null ? "" : (":" + ex.getMessage()));
            if (reason.length() > 120) {
                reason = reason.substring(0, 120);
            }
            persistActionByQsId(qsId, "冻结二维码失败: " + reason);
            // keep alert path non-blocking when trace service is unstable
        }
    }

    private boolean sendNotification(AlertRecord record) {
        // TODO: replace with real message/SMS/email adapter.
        return record != null;
    }

    private void persistAlertRecord(AlertRecord record, RuleEngineResult ruleResult, RiskLevel level) {
        if (level == RiskLevel.LOW) {
            return;
        }
        String ruleId = ruleResult.hitRuleIds().isEmpty() ? "R001" : ruleResult.hitRuleIds().get(0);
        String reason = ruleResult.hitReasons().isEmpty() ? "风险评分触发" : ruleResult.hitReasons().get(0);
        String status = "open";
        int alertLevel = toAlertLevel(level);

        jdbcTemplate.update(
                "INSERT INTO alert_record(qs_id,company_id,rule_id,alert_level,reason,detail,created_at,status) VALUES (?,?,?,?,?,?,NOW(),?)",
                record.getQsId(),
                record.getCompanyId(),
                ruleId,
                alertLevel,
                reason,
                record.getDetail(),
                status
        );
    }

    private void persistNotifyAction(AlertRecord record, String result) {
        Long alertId = jdbcTemplate.query(
                "SELECT alert_id FROM alert_record WHERE qs_id=? ORDER BY alert_id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("alert_id") : null,
                record.getQsId()
        );
        if (alertId == null) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO alert_action(alert_id,action_type,result,push_status,retry_count) VALUES (?,?,?,?,?)",
                alertId,
                "notify",
                result,
                result.contains("失败") ? "fail" : "success",
                Math.max(0, record.getNotifyRetry())
        );
    }

    private void persistActionByQsId(String qsId, String result) {
        Long alertId = jdbcTemplate.query(
                "SELECT alert_id FROM alert_record WHERE qs_id=? ORDER BY alert_id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("alert_id") : null,
                qsId
        );
        if (alertId == null) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO alert_action(alert_id,action_type,result,push_status,retry_count) VALUES (?,?,?,?,?)",
                alertId,
                "freeze",
                result,
                result.contains("失败") ? "fail" : "success",
                0
        );
    }

    private int firstNonNull(Integer preferred, Integer fallback) {
        if (preferred != null) {
            return preferred;
        }
        if (fallback != null) {
            return fallback;
        }
        return 0;
    }

    private int toAlertLevel(RiskLevel level) {
        if (level == RiskLevel.HIGH) {
            return 1;
        }
        if (level == RiskLevel.MEDIUM) {
            return 2;
        }
        return 3;
    }

    private RiskLevel toRiskLevel(int alertLevel) {
        if (alertLevel == 1) {
            return RiskLevel.HIGH;
        }
        if (alertLevel == 2) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
