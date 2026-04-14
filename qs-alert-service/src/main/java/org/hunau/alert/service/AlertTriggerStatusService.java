package org.hunau.alert.service;

import org.hunau.common.enums.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AlertTriggerStatusService {

    private static final Logger log = LoggerFactory.getLogger(AlertTriggerStatusService.class);

    private final JdbcTemplate jdbcTemplate;

    public AlertTriggerStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TriggerStatusResult checkAndUpdateTriggerStatus(String qsId, String companyId, String ruleId, RiskLevel riskLevel, Long alertId) {
        TriggerStatusRecord existing = findExisting(qsId, ruleId);
        
        if (existing == null) {
            return createNewStatus(qsId, companyId, ruleId, riskLevel, alertId);
        }
        
        return updateExistingStatus(existing, qsId, companyId, ruleId, riskLevel, alertId);
    }

    private TriggerStatusRecord findExisting(String qsId, String ruleId) {
        return jdbcTemplate.query(
                "SELECT id, qs_id, company_id, rule_id, risk_level, first_trigger_time, " +
                        "last_trigger_time, trigger_count, last_alert_id, status " +
                        "FROM alert_trigger_status WHERE qs_id=? AND rule_id=? AND status='ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    TriggerStatusRecord record = new TriggerStatusRecord();
                    record.setId(rs.getLong("id"));
                    record.setQsId(rs.getString("qs_id"));
                    record.setCompanyId(rs.getString("company_id"));
                    record.setRuleId(rs.getString("rule_id"));
                    record.setRiskLevel(rs.getString("risk_level"));
                    record.setFirstTriggerTime(rs.getTimestamp("first_trigger_time").toLocalDateTime());
                    record.setLastTriggerTime(rs.getTimestamp("last_trigger_time").toLocalDateTime());
                    record.setTriggerCount(rs.getInt("trigger_count"));
                    record.setLastAlertId(rs.getLong("last_alert_id"));
                    record.setStatus(rs.getString("status"));
                    return record;
                },
                qsId, ruleId
        );
    }

    private TriggerStatusRecord findAny(String qsId, String ruleId) {
        return jdbcTemplate.query(
                "SELECT id, qs_id, company_id, rule_id, risk_level, first_trigger_time, " +
                        "last_trigger_time, trigger_count, last_alert_id, status " +
                        "FROM alert_trigger_status WHERE qs_id=? AND rule_id=?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    TriggerStatusRecord record = new TriggerStatusRecord();
                    record.setId(rs.getLong("id"));
                    record.setQsId(rs.getString("qs_id"));
                    record.setCompanyId(rs.getString("company_id"));
                    record.setRuleId(rs.getString("rule_id"));
                    record.setRiskLevel(rs.getString("risk_level"));
                    record.setFirstTriggerTime(rs.getTimestamp("first_trigger_time").toLocalDateTime());
                    record.setLastTriggerTime(rs.getTimestamp("last_trigger_time").toLocalDateTime());
                    record.setTriggerCount(rs.getInt("trigger_count"));
                    record.setLastAlertId(rs.getLong("last_alert_id"));
                    record.setStatus(rs.getString("status"));
                    return record;
                },
                qsId, ruleId
        );
    }

    private TriggerStatusResult createNewStatus(String qsId, String companyId, String ruleId, RiskLevel riskLevel, Long alertId) {
        jdbcTemplate.update(
                "DELETE FROM alert_trigger_status WHERE qs_id=? AND rule_id=? AND status='RESOLVED'",
                qsId, ruleId
        );
        
        LocalDateTime now = LocalDateTime.now();
        int affected = jdbcTemplate.update(
                "INSERT INTO alert_trigger_status(qs_id, company_id, rule_id, risk_level, " +
                        "first_trigger_time, last_trigger_time, trigger_count, last_alert_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')",
                qsId, companyId, ruleId, riskLevel.name(), now, now, 1, alertId
        );
        
        if (affected > 0) {
            log.info("Created new trigger status: qsId={}, ruleId={}, riskLevel={}, alertId={}", 
                    qsId, ruleId, riskLevel, alertId);
        }
        
        return new TriggerStatusResult(false, 1, now, null);
    }

    private TriggerStatusResult updateExistingStatus(TriggerStatusRecord existing, String qsId, 
                                                     String companyId, String ruleId, 
                                                     RiskLevel riskLevel, Long alertId) {
        LocalDateTime now = LocalDateTime.now();
        int newCount = existing.getTriggerCount() + 1;
        
        int affected = jdbcTemplate.update(
                "UPDATE alert_trigger_status SET last_trigger_time=?, trigger_count=?, " +
                        "last_alert_id=?, risk_level=? WHERE id=?",
                now, newCount, alertId, riskLevel.name(), existing.getId()
        );
        
        if (affected > 0) {
            log.info("Updated existing trigger status: qsId={}, ruleId={}, triggerCount={}, " +
                    "lastAlertId={}, previousAlertId={}", 
                    qsId, ruleId, newCount, alertId, existing.getLastAlertId());
        }
        
        return new TriggerStatusResult(true, newCount, now, existing.getLastAlertId());
    }

    public void resolveStatus(String qsId, String ruleId) {
        int affected = jdbcTemplate.update(
                "UPDATE alert_trigger_status SET status='RESOLVED', updated_at=NOW() " +
                        "WHERE qs_id=? AND rule_id=? AND status='ACTIVE'",
                qsId, ruleId
        );
        
        if (affected > 0) {
            log.info("Resolved trigger status: qsId={}, ruleId={}", qsId, ruleId);
        }
    }

    public Optional<TriggerStatusRecord> getStatus(String qsId, String ruleId) {
        TriggerStatusRecord record = findExisting(qsId, ruleId);
        return Optional.ofNullable(record);
    }

    public static class TriggerStatusRecord {
        private Long id;
        private String qsId;
        private String companyId;
        private String ruleId;
        private String riskLevel;
        private LocalDateTime firstTriggerTime;
        private LocalDateTime lastTriggerTime;
        private int triggerCount;
        private Long lastAlertId;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getQsId() { return qsId; }
        public void setQsId(String qsId) { this.qsId = qsId; }
        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }
        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public LocalDateTime getFirstTriggerTime() { return firstTriggerTime; }
        public void setFirstTriggerTime(LocalDateTime firstTriggerTime) { this.firstTriggerTime = firstTriggerTime; }
        public LocalDateTime getLastTriggerTime() { return lastTriggerTime; }
        public void setLastTriggerTime(LocalDateTime lastTriggerTime) { this.lastTriggerTime = lastTriggerTime; }
        public int getTriggerCount() { return triggerCount; }
        public void setTriggerCount(int triggerCount) { this.triggerCount = triggerCount; }
        public Long getLastAlertId() { return lastAlertId; }
        public void setLastAlertId(Long lastAlertId) { this.lastAlertId = lastAlertId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public record TriggerStatusResult(
            boolean alreadyTriggered,
            int triggerCount,
            LocalDateTime lastTriggerTime,
            Long previousAlertId
    ) {}
}