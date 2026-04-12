CREATE TABLE IF NOT EXISTS alert_rule (
    rule_id VARCHAR(32) PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_content TEXT NOT NULL,
    threshold VARCHAR(50),
    alert_level TINYINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO alert_rule(rule_id, rule_name, rule_content, threshold, alert_level, status) VALUES 
    ('R001', '高频扫码', 'IF scan_count_1h >= threshold THEN alert', '1h>=5', 2, 1),
    ('R002', '多设备扫码', 'IF device_count_1d >= threshold THEN alert', '1d>=10', 2, 1),
    ('R003', '多IP扫码', 'IF ip_count_1h >= threshold THEN alert', '1h>=20', 1, 1);

CREATE TABLE IF NOT EXISTS alert_record (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qs_id VARCHAR(32) NOT NULL,
    company_id VARCHAR(32) NOT NULL,
    rule_id VARCHAR(32) NOT NULL,
    alert_level TINYINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    detail TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(10) DEFAULT 'open'
);

CREATE TABLE IF NOT EXISTS alert_action (
    action_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    result VARCHAR(200),
    push_status VARCHAR(20),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
);