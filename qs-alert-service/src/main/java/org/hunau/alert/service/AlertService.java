package org.hunau.alert.service;

import org.hunau.alert.AiRiskService;
import org.hunau.alert.client.BlockFeignClient;
import org.hunau.alert.client.TraceFeignClient;
import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.AlertRecord;
import org.hunau.alert.model.FeedbackMessageRequest;
import org.hunau.alert.rule.RuleEngineResult;
import org.hunau.common.R;
import org.hunau.common.RiskLevel;
import org.hunau.common.util.AssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AiRiskService aiRiskService;
    private final AlertRuleEngineService alertRuleEngineService;
    private final AlertTriggerStatusService alertTriggerStatusService;
    private final TraceFeignClient traceFeignClient;
    private final BlockFeignClient blockFeignClient;
    private final JdbcTemplate jdbcTemplate;

    public AlertService(AiRiskService aiRiskService,
                        AlertRuleEngineService alertRuleEngineService,
                        AlertTriggerStatusService alertTriggerStatusService,
                        TraceFeignClient traceFeignClient,
                        BlockFeignClient blockFeignClient,
                        JdbcTemplate jdbcTemplate) {
        this.aiRiskService = aiRiskService;
        this.alertRuleEngineService = alertRuleEngineService;
        this.alertTriggerStatusService = alertTriggerStatusService;
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

        String ruleId = ruleResult.hitRuleIds().isEmpty() ? "R001" : ruleResult.hitRuleIds().get(0);
        
        persistAlertRecord(record, ruleResult, level);
        
        Long alertId = jdbcTemplate.query(
                "SELECT alert_id FROM alert_record WHERE qs_id=? ORDER BY alert_id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("alert_id") : null,
                record.getQsId()
        );

        if (level != RiskLevel.LOW) {
            AlertTriggerStatusService.TriggerStatusResult triggerResult = 
                    alertTriggerStatusService.checkAndUpdateTriggerStatus(
                            record.getQsId(), 
                            record.getCompanyId(), 
                            ruleId, 
                            level, 
                            alertId
                    );
            
            if (triggerResult.alreadyTriggered()) {
                log.info("Alert already triggered for qsId={}, ruleId={}, updating existing status only. " +
                        "Trigger count: {}, previous alertId: {}", 
                        record.getQsId(), ruleId, triggerResult.triggerCount(), triggerResult.previousAlertId());
                
                updateExistingAlertRecord(record, ruleResult, alertId);
            } else {
                log.info("First alert trigger for qsId={}, ruleId={}, sending notification", 
                        record.getQsId(), ruleId);
                
                notifyRegulator(record);
                saveEventProof(record);
            }
        }
        if (level == RiskLevel.HIGH) {
            freezeQsCode(record.getQsId());
        }

        return R.ok(record);
    }

    private void updateExistingAlertRecord(AlertRecord record, RuleEngineResult ruleResult, Long alertId) {
        if (alertId == null) {
            return;
        }
        String reason = buildAlertReason(ruleResult, record.getRiskScore());
        
        jdbcTemplate.update(
                "UPDATE alert_record SET detail=?, reason=?, updated_at=NOW() WHERE alert_id=?",
                record.getDetail(),
                reason,
                alertId
        );
        
        jdbcTemplate.update(
                "INSERT INTO alert_action(alert_id,action_type,result,push_status,retry_count) VALUES (?,?,?,?,?)",
                alertId,
                "update",
                "告警内容已更新",
                "success",
                0
        );
        
        log.info("Updated existing alert record: alertId={}, qsId={}, reason={}", alertId, record.getQsId(), reason);
    }

    public R<List<Map<String, Object>>> list(String role, String companyId) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        String normalizedCompanyId = companyId == null ? "" : companyId.trim();

        String baseSql = "SELECT ar.alert_id,ar.qs_id,ar.company_id,ar.alert_level,ar.reason,ar.detail,ar.created_at,ar.status," +
                "aa.result AS action_result,aa.push_status AS push_status, " +
                "ar.batch_id, ar.batch_name " +
                "FROM alert_record ar " +
                "LEFT JOIN (" +
                "  SELECT t1.alert_id,t1.result,t1.push_status FROM alert_action t1 " +
                "  INNER JOIN (SELECT alert_id,MAX(action_id) max_id FROM alert_action GROUP BY alert_id) t2 " +
                "    ON t1.alert_id=t2.alert_id AND t1.action_id=t2.max_id" +
                ") aa ON ar.alert_id=aa.alert_id ";

        List<Map<String, Object>> result;
        if (!normalizedCompanyId.isBlank()) {
            result = jdbcTemplate.query(
                    baseSql + "WHERE ar.company_id=? ORDER BY ar.created_at DESC LIMIT 500",
                    (rs, rowNum) -> mapMessageRowWithRead(rs, normalizedCompanyId),
                    normalizedCompanyId
            );
        } else if ("COMPANY".equals(normalizedRole)) {
            result = new ArrayList<>();
        } else {
            result = jdbcTemplate.query(
                    baseSql + "ORDER BY ar.created_at DESC LIMIT 500",
                    (rs, rowNum) -> mapMessageRowWithRead(rs, null)
            );
        }

        return R.ok(groupByBatch(result));
    }

    public R<Map<String, Object>> countUnread(String companyId) {
        String normalizedCompanyId = companyId == null ? "" : companyId.trim();
        Map<String, Object> result = new HashMap<>();
        
        if (!normalizedCompanyId.isBlank()) {
            String sql = "SELECT COUNT(*) as count FROM alert_record ar " +
                    "WHERE ar.status='open' AND ar.company_id=? " +
                    "AND NOT EXISTS (SELECT 1 FROM alert_read r WHERE r.alert_id=ar.alert_id AND r.company_id=?)";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, normalizedCompanyId, normalizedCompanyId);
            result.put("unreadCount", count != null ? count : 0);
        } else {
            String sql = "SELECT COUNT(*) as count FROM alert_record WHERE status='open'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            result.put("unreadCount", count != null ? count : 0);
        }
        
        return R.ok(result);
    }

    public R<Map<String, Object>> markAsRead(Long alertId, String companyId) {
        String normalizedCompanyId = companyId == null ? "" : companyId.trim();
        AssertUtil.notNull(alertId, "alertId不能为空");
        AssertUtil.notEmpty(normalizedCompanyId, "companyId不能为空");
        
        jdbcTemplate.update(
                "INSERT IGNORE INTO alert_read(alert_id, company_id, read_time) VALUES (?, ?, NOW())",
                alertId,
                normalizedCompanyId
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("alertId", alertId);
        result.put("marked", true);
        return R.ok(result);
    }

    public R<Map<String, Object>> markAllAsRead(String companyId) {
        String normalizedCompanyId = companyId == null ? "" : companyId.trim();
        AssertUtil.notEmpty(normalizedCompanyId, "companyId不能为空");
        
        jdbcTemplate.update(
                "INSERT INTO alert_read(alert_id, company_id, read_time) " +
                "SELECT ar.alert_id, ?, NOW() FROM alert_record ar " +
                "WHERE ar.company_id=? AND ar.status='open' " +
                "AND NOT EXISTS (SELECT 1 FROM alert_read r WHERE r.alert_id=ar.alert_id AND r.company_id=?)",
                normalizedCompanyId,
                normalizedCompanyId,
                normalizedCompanyId
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("marked", true);
        return R.ok(result);
    }

    private List<Map<String, Object>> groupByBatch(List<Map<String, Object>> alerts) {
        Map<String, Map<String, Object>> batchGroups = new LinkedHashMap<>();
        
        for (Map<String, Object> alert : alerts) {
            String batchId = (String) alert.getOrDefault("batchId", "");
            String batchName = (String) alert.getOrDefault("batchName", "未命名批次");
            
            if (batchId == null || batchId.isBlank()) {
                batchId = "_unassigned";
            }
            
            if (!batchGroups.containsKey(batchId)) {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("batchId", "_unassigned".equals(batchId) ? null : batchId);
                group.put("batchName", batchName);
                group.put("alerts", new ArrayList<Map<String, Object>>());
                group.put("highRiskCount", 0);
                group.put("mediumRiskCount", 0);
                batchGroups.put(batchId, group);
            }
            
            Map<String, Object> group = batchGroups.get(batchId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alertList = (List<Map<String, Object>>) group.get("alerts");
            alertList.add(alert);
            
            String alertLevel = (String) alert.get("alertLevel");
            if ("HIGH".equals(alertLevel)) {
                group.put("highRiskCount", (Integer) group.get("highRiskCount") + 1);
            } else if ("MEDIUM".equals(alertLevel)) {
                group.put("mediumRiskCount", (Integer) group.get("mediumRiskCount") + 1);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>(batchGroups.values());
        result.sort((a, b) -> {
            int highA = (Integer) a.get("highRiskCount");
            int highB = (Integer) b.get("highRiskCount");
            if (highB != highA) return highB - highA;
            
            int mediumA = (Integer) a.get("mediumRiskCount");
            int mediumB = (Integer) b.get("mediumRiskCount");
            return mediumB - mediumA;
        });
        
        return result;
    }

    private Map<String, Object> mapMessageRowWithRead(java.sql.ResultSet rs, String companyId) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("alertId", rs.getLong("alert_id"));
        row.put("qsId", rs.getString("qs_id"));
        row.put("companyId", rs.getString("company_id"));
        row.put("alertLevel", toRiskLevel(rs.getInt("alert_level")).name());
        row.put("reason", rs.getString("reason"));
        row.put("detail", rs.getString("detail"));
        row.put("status", "open".equalsIgnoreCase(rs.getString("status")) ? "OPEN" : "CLOSED");
        row.put("actionResult", rs.getString("action_result"));
        row.put("pushStatus", rs.getString("push_status"));
        row.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        row.put("batchId", rs.getString("batch_id"));
        row.put("batchName", rs.getString("batch_name"));
        
        if (companyId != null && !companyId.isBlank()) {
            Boolean isRead = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM alert_read WHERE alert_id=? AND company_id=?)",
                    Boolean.class,
                    rs.getLong("alert_id"),
                    companyId
            );
            row.put("isRead", isRead != null && isRead);
        }
        
        return row;
    }

    public R<Map<String, Object>> createFeedbackMessage(FeedbackMessageRequest request) {
        AssertUtil.notNull(request, "请求不能为空");
        AssertUtil.notEmpty(request.getQsId(), "qsId不能为空");
        AssertUtil.notEmpty(request.getCompanyId(), "companyId不能为空");
        AssertUtil.notEmpty(request.getRiskLevel(), "riskLevel不能为空");

        String riskLevel = request.getRiskLevel().trim().toUpperCase(Locale.ROOT);
        int alertLevel = toAlertLevelByText(riskLevel);
        if (alertLevel < 0) {
            return R.fail("riskLevel仅支持 HIGH/MEDIUM");
        }

        double complaintRate = Optional.ofNullable(request.getComplaintRate()).orElse(0.0);
        String reason = "消费者反馈投诉率异常(" + String.format(Locale.ROOT, "%.2f%%", complaintRate * 100.0) + ")";
        String detail = "feedbackId=" + safeText(request.getFeedbackId())
                + ", feedbackType=" + safeText(request.getFeedbackType())
                + ", complaintRate=" + String.format(Locale.ROOT, "%.6f", complaintRate)
                + ", source=feedback_flow";

        Map<String, Object> qsInfo = fetchQsCodeInfo(request.getQsId());
        String batchId = (String) qsInfo.get("batchId");
        String batchName = (String) qsInfo.get("batchName");

        jdbcTemplate.update(
                "INSERT INTO alert_record(qs_id,company_id,rule_id,alert_level,reason,detail,created_at,status,batch_id,batch_name) VALUES (?,?,?,?,?,?,NOW(),'open',?,?)",
                request.getQsId().trim(),
                request.getCompanyId().trim(),
                "R001",
                alertLevel,
                reason,
                detail,
                batchId,
                batchName
        );

        Long alertId = jdbcTemplate.query(
                "SELECT alert_id FROM alert_record WHERE qs_id=? AND company_id=? ORDER BY alert_id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("alert_id") : null,
                request.getQsId().trim(),
                request.getCompanyId().trim()
        );
        if (alertId != null) {
            jdbcTemplate.update(
                    "INSERT INTO alert_action(alert_id,action_type,result,push_status,retry_count) VALUES (?,?,?,?,?)",
                    alertId,
                    "notify",
                    "反馈投诉率触发系统消息",
                    "success",
                    0
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertId", alertId);
        payload.put("qsId", request.getQsId().trim());
        payload.put("companyId", request.getCompanyId().trim());
        payload.put("riskLevel", riskLevel);
        payload.put("complaintRate", complaintRate);
        payload.put("created", true);
        return R.ok(payload);
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
        String reason = buildAlertReason(ruleResult, record.getRiskScore());
        String status = "open";
        int alertLevel = toAlertLevel(level);

        String batchId = record.getBatchId();
        String batchName = record.getBatchName();
        
        if (batchId == null || batchId.isBlank()) {
            Map<String, Object> qsInfo = fetchQsCodeInfo(record.getQsId());
            batchId = (String) qsInfo.get("batchId");
            batchName = (String) qsInfo.get("batchName");
        }

        jdbcTemplate.update(
                "INSERT INTO alert_record(qs_id,company_id,rule_id,alert_level,reason,detail,created_at,status,batch_id,batch_name) VALUES (?,?,?,?,?,?,NOW(),?,?,?)",
                record.getQsId(),
                record.getCompanyId(),
                ruleId,
                alertLevel,
                reason,
                record.getDetail(),
                status,
                batchId,
                batchName
        );
    }

    private Map<String, Object> fetchQsCodeInfo(String qsId) {
        Map<String, Object> result = new HashMap<>();
        try {
            R<?> response = traceFeignClient.getQsCodeDetail(qsId);
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getData();
                Map<String, Object> batch = (Map<String, Object>) data.get("batch");
                if (batch != null) {
                    result.put("batchId", batch.get("batchId"));
                    result.put("batchName", batch.get("batchName"));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch QS code info for qsId={}: {}", qsId, e.getMessage());
        }
        if (result.get("batchId") == null) {
            result.put("batchId", "");
        }
        if (result.get("batchName") == null) {
            result.put("batchName", "未知批次");
        }
        return result;
    }

    private String buildAlertReason(RuleEngineResult ruleResult, double riskScore) {
        if (!ruleResult.hitRuleIds().isEmpty() && !ruleResult.hitReasons().isEmpty()) {
            return "规则引擎判定: " + ruleResult.hitReasons().get(0);
        }
        
        if (riskScore >= 0.75) {
            return "AI模型判定: 高风险评分(" + String.format("%.2f", riskScore) + ")";
        }
        if (riskScore >= 0.45) {
            return "AI模型判定: 中等风险评分(" + String.format("%.2f", riskScore) + ")";
        }
        
        return "风险评分触发(" + String.format("%.2f", riskScore) + ")";
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

    private int toAlertLevelByText(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return 1;
        }
        if ("MEDIUM".equals(riskLevel)) {
            return 2;
        }
        return -1;
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private Map<String, Object> mapMessageRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("alertId", rs.getLong("alert_id"));
        row.put("qsId", rs.getString("qs_id"));
        row.put("companyId", rs.getString("company_id"));
        row.put("alertLevel", toRiskLevel(rs.getInt("alert_level")).name());
        row.put("reason", rs.getString("reason"));
        row.put("detail", rs.getString("detail"));
        row.put("status", "open".equalsIgnoreCase(rs.getString("status")) ? "OPEN" : "CLOSED");
        row.put("actionResult", rs.getString("action_result"));
        row.put("pushStatus", rs.getString("push_status"));
        row.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        return row;
    }

    private Map<String, Object> mapMessageRowWithBatch(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("alertId", rs.getLong("alert_id"));
        row.put("qsId", rs.getString("qs_id"));
        row.put("companyId", rs.getString("company_id"));
        row.put("alertLevel", toRiskLevel(rs.getInt("alert_level")).name());
        row.put("reason", rs.getString("reason"));
        row.put("detail", rs.getString("detail"));
        row.put("status", "open".equalsIgnoreCase(rs.getString("status")) ? "OPEN" : "CLOSED");
        row.put("actionResult", rs.getString("action_result"));
        row.put("pushStatus", rs.getString("push_status"));
        row.put("createdAt", rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        row.put("batchId", rs.getString("batch_id"));
        row.put("batchName", rs.getString("batch_name"));
        return row;
    }
}
