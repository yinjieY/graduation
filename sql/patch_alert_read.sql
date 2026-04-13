USE `yx_alert_engine`;

DROP TABLE IF EXISTS `alert_read`;

CREATE TABLE `alert_read` (
    `read_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '已读记录自增ID',
    `alert_id` BIGINT NOT NULL COMMENT '关联预警记录ID',
    `company_id` VARCHAR(32) NOT NULL COMMENT '企业ID',
    `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    KEY `idx_alert_read` (`alert_id`),
    KEY `idx_company_read` (`company_id`),
    UNIQUE KEY `uk_alert_company` (`alert_id`, `company_id`),
    CONSTRAINT `fk_read_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_record` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警消息已读记录表';
