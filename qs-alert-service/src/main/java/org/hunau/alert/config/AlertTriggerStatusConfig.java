package org.hunau.alert.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertTriggerStatusConfig {

    private static final Logger log = LoggerFactory.getLogger(AlertTriggerStatusConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public AlertTriggerStatusConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            ensureAlertRecordUpdatedAtColumn();
            ensureTriggerStatusTable();
        } catch (Exception e) {
            log.warn("Failed to initialize alert trigger status table, may be running in test environment: {}", e.getMessage());
        }
    }

    private void ensureAlertRecordUpdatedAtColumn() {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'alert_record' AND column_name = 'updated_at'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE alert_record ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                log.info("Added updated_at column to alert_record table");
            }
        } catch (Exception e) {
            log.warn("Failed to add updated_at column to alert_record table: {}", e.getMessage());
        }
    }

    private void ensureTriggerStatusTable() {
        String dbType = getDatabaseType();
        String createTableSql;
        
        if ("H2".equals(dbType)) {
            createTableSql = """
                    CREATE TABLE IF NOT EXISTS alert_trigger_status (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        qs_id VARCHAR(64) NOT NULL,
                        company_id VARCHAR(64) NOT NULL,
                        rule_id VARCHAR(32) NOT NULL,
                        risk_level VARCHAR(16) NOT NULL,
                        first_trigger_time TIMESTAMP NOT NULL,
                        last_trigger_time TIMESTAMP NOT NULL,
                        trigger_count INT NOT NULL DEFAULT 1,
                        last_alert_id BIGINT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT uk_qs_rule UNIQUE (qs_id, rule_id)
                    )
                    """;
        } else {
            createTableSql = """
                    CREATE TABLE IF NOT EXISTS alert_trigger_status (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
                        qs_id VARCHAR(64) NOT NULL COMMENT '二维码ID',
                        company_id VARCHAR(64) NOT NULL COMMENT '企业ID',
                        rule_id VARCHAR(32) NOT NULL COMMENT '触发的规则ID',
                        risk_level VARCHAR(16) NOT NULL COMMENT '风险等级(HIGH/MEDIUM/LOW)',
                        first_trigger_time DATETIME NOT NULL COMMENT '首次触发时间',
                        last_trigger_time DATETIME NOT NULL COMMENT '最后触发时间',
                        trigger_count INT NOT NULL DEFAULT 1 COMMENT '触发次数',
                        last_alert_id BIGINT NULL COMMENT '最后关联的告警ID',
                        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/RESOLVED)',
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        UNIQUE KEY uk_qs_rule (qs_id, rule_id),
                        KEY idx_company_id (company_id),
                        KEY idx_last_trigger_time (last_trigger_time)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警触发状态记录表'
                    """;
        }
        
        jdbcTemplate.execute(createTableSql);
        log.info("alert_trigger_status table initialized successfully for database type: {}", dbType);
    }

    private String getDatabaseType() {
        try {
            return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class) != null ? "MySQL" : "H2";
        } catch (Exception e) {
            return "H2";
        }
    }
}