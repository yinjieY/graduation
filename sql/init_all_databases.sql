-- ==================================================
-- QSGuard Multi-DB Initialization Script (Unified)
-- Target: MySQL 5.7+ / 8.0+
-- Purpose:
--   1) Unify company_id rule to C001/C002 style
--   2) Provide login-ready BCrypt seed users
--   3) Relax blockchain proof uniqueness and align with code fields
-- ==================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==================================================
-- 库 1：yx_trace_core 核心溯源库
-- ==================================================
DROP DATABASE IF EXISTS `yx_trace_core`;
CREATE DATABASE `yx_trace_core` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_trace_core`;

DROP TABLE IF EXISTS `qs_code`;
DROP TABLE IF EXISTS `product_batch`;
DROP TABLE IF EXISTS `company`;

CREATE TABLE `company` (
    `company_id` VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '企业唯一ID',
    `name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `level` ENUM('特级','一级') DEFAULT '一级' COMMENT '企业等级',
    `address` VARCHAR(255) NOT NULL COMMENT '生产地址',
    `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话（脱敏）',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `company` (`company_id`,`name`,`level`,`address`,`contact_phone`,`status`) VALUES
('C001','攸县老灶香干厂','特级','湖南株洲攸县','138****1234',1),
('C002','湘东豆制品有限公司','一级','湖南株洲攸县','139****5678',1);

CREATE TABLE `product_batch` (
    `batch_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '批次唯一ID',
    `company_id` VARCHAR(32) NOT NULL COMMENT '所属企业ID',
    `production_date` DATETIME NOT NULL COMMENT '生产日期',
    `ingredients` VARCHAR(200) NOT NULL COMMENT '配料信息',
    `production_standard` VARCHAR(100) NOT NULL COMMENT '执行生产标准号',
    `total_quantity` INT NOT NULL COMMENT '批次总产量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_batch_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `product_batch` (`batch_id`,`company_id`,`production_date`,`ingredients`,`production_standard`,`total_quantity`) VALUES
('BATCH2026_C001_001','C001',NOW(),'黄豆、水','GB/T 22106-2008',500),
('BATCH2026_C002_001','C002',NOW(),'黄豆、辣椒','GB/T 22106-2008',300);

CREATE TABLE `qs_code` (
    `qs_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '二维码唯一ID',
    `batch_id` VARCHAR(32) NOT NULL COMMENT '关联批次ID',
    `company_id` VARCHAR(32) NOT NULL COMMENT '所属企业ID',
    `qs_url` VARCHAR(255) NOT NULL COMMENT '二维码访问地址',
    `sm2_sign` VARCHAR(256) NOT NULL COMMENT 'SM2国密签名值',
    `issue_time` DATETIME NOT NULL COMMENT '发放时间',
    `status` ENUM('active','invalid','frozen','cancelled') DEFAULT 'active',
    `max_allowed_scans` INT NOT NULL DEFAULT 5,
    `freeze_time` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_qs_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`),
    CONSTRAINT `fk_qs_batch` FOREIGN KEY (`batch_id`) REFERENCES `product_batch` (`batch_id`),
    KEY `idx_qs_company` (`company_id`),
    KEY `idx_qs_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `qs_code` (`qs_id`,`batch_id`,`company_id`,`qs_url`,`sm2_sign`,`issue_time`,`status`,`max_allowed_scans`) VALUES
('QS000001','BATCH2026_C001_001','C001','http://trace.local/qs/QS000001','sign123',NOW(),'active',5),
('QS000002','BATCH2026_C002_001','C002','http://trace.local/qs/QS000002','sign456',NOW(),'active',5);

-- ==================================================
-- 库 2：yx_scan_anomaly 扫码行为库
-- ==================================================
DROP DATABASE IF EXISTS `yx_scan_anomaly`;
CREATE DATABASE `yx_scan_anomaly` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_scan_anomaly`;

DROP TABLE IF EXISTS `scan_log`;
CREATE TABLE `scan_log` (
    `scan_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `qs_id` VARCHAR(32) NOT NULL,
    `scan_time` DATETIME NOT NULL,
    `ip` VARCHAR(45) NOT NULL,
    `ip_masked` VARCHAR(45) NOT NULL,
    `device_fingerprint` VARCHAR(64) NOT NULL,
    `lat` DOUBLE NULL,
    `lng` DOUBLE NULL,
    `city` VARCHAR(50) NULL,
    `province` VARCHAR(50) NULL,
    `is_first` TINYINT(1) NOT NULL DEFAULT 0,
    `phone_masked` VARCHAR(20) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_qs_time` (`qs_id`, `scan_time`),
    KEY `idx_device` (`device_fingerprint`),
    KEY `idx_scan_time` (`scan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `scan_log` (`qs_id`,`scan_time`,`ip`,`ip_masked`,`device_fingerprint`,`lat`,`lng`,`city`,`province`,`is_first`,`created_at`) VALUES
('QS000001',NOW(),'127.0.0.1','127.0.0.*','device1',27.24,113.77,'攸县','湖南',1,NOW()),
('QS000002',NOW(),'127.0.0.1','127.0.0.*','device2',27.86,113.13,'株洲','湖南',0,NOW());

-- ==================================================
-- 库 3：yx_alert_engine 预警引擎库
-- ==================================================
DROP DATABASE IF EXISTS `yx_alert_engine`;
CREATE DATABASE `yx_alert_engine` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_alert_engine`;

DROP TABLE IF EXISTS `alert_action`;
DROP TABLE IF EXISTS `alert_record`;
DROP TABLE IF EXISTS `alert_rule`;

CREATE TABLE `alert_rule` (
    `rule_id` VARCHAR(32) NOT NULL PRIMARY KEY,
    `rule_name` VARCHAR(100) NOT NULL,
    `rule_content` TEXT NOT NULL,
    `threshold` VARCHAR(50) NOT NULL,
    `alert_level` TINYINT NOT NULL COMMENT '1严重 2中等 3轻微',
    `status` TINYINT(1) NOT NULL DEFAULT 1,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_rule_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `alert_rule` (`rule_id`,`rule_name`,`rule_content`,`threshold`,`alert_level`,`status`) VALUES
('R001','高频扫码','IF scans_1h >= 5 THEN alert','1h>=5',2,1);

CREATE TABLE `alert_record` (
    `alert_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `qs_id` VARCHAR(32) NOT NULL,
    `company_id` VARCHAR(32) NOT NULL,
    `rule_id` VARCHAR(32) NOT NULL,
    `alert_level` TINYINT NOT NULL,
    `reason` VARCHAR(200) NOT NULL,
    `detail` TEXT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('open','closed') DEFAULT 'open',
    `handle_user` VARCHAR(50) NULL,
    `handle_time` DATETIME NULL,
    `handle_note` VARCHAR(200) NULL,
    KEY `idx_company_level` (`company_id`, `alert_level`),
    KEY `idx_qs` (`qs_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_alert_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `alert_record` (`qs_id`,`company_id`,`rule_id`,`alert_level`,`reason`,`detail`) VALUES
('QS000001','C001','R001',2,'疑似复用','扫码次数超标');

CREATE TABLE `alert_action` (
    `action_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `alert_id` BIGINT NOT NULL,
    `action_type` ENUM('freeze','notify','blockchain_record') NOT NULL,
    `action_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `result` VARCHAR(200) NOT NULL,
    `push_status` ENUM('success','fail','retry') DEFAULT 'success',
    `retry_count` TINYINT(1) DEFAULT 0,
    KEY `idx_alert_action` (`alert_id`),
    CONSTRAINT `fk_action_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_record` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `alert_action` (`alert_id`,`action_type`,`result`) VALUES
(1,'notify','发送预警成功');

-- ==================================================
-- 库 4：yx_company_auth 企业认证与登录库
-- ==================================================
DROP DATABASE IF EXISTS `yx_company_auth`;
CREATE DATABASE `yx_company_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yx_company_auth`;

DROP TABLE IF EXISTS `company_auth`;
DROP TABLE IF EXISTS `auth_user`;

CREATE TABLE `auth_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` VARCHAR(32) NOT NULL COMMENT 'ADMIN/COMPANY/CONSUMER/SERVICE',
  `company_id` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_username` (`username`),
  KEY `idx_auth_user_role` (`role`),
  KEY `idx_auth_user_company_id` (`company_id`),
  KEY `idx_auth_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `company_auth` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `company_id` VARCHAR(32) NOT NULL,
  `company_name` VARCHAR(128) DEFAULT NULL,
  `review_status` TINYINT NOT NULL DEFAULT 0,
  `review_by` VARCHAR(64) DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_auth_company_id` (`company_id`),
  KEY `idx_company_auth_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `company_auth` (`company_id`,`company_name`,`review_status`,`review_by`,`review_time`,`remark`) VALUES
('C001','攸县老灶香干厂',1,'admin001',NOW(),'Initialized as approved'),
('C002','湘东豆制品有限公司',0,'admin001',NOW(),'Initialized as not approved');

-- initial password: password
-- bcrypt: $2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa
INSERT INTO `auth_user` (`username`,`password_hash`,`role`,`company_id`,`status`,`deleted`) VALUES
('admin001',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'ADMIN', NULL,   1, 0),
('company01', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'COMPANY','C001', 1, 0),
('consumer01','$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'CONSUMER',NULL,  1, 0);

-- ==================================================
-- 库 5：yx_blockchain_proof 区块链存证库
-- NOTE: aligned with current block-service code
-- ==================================================
DROP DATABASE IF EXISTS `yx_blockchain_proof`;
CREATE DATABASE `yx_blockchain_proof` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yx_blockchain_proof`;

DROP TABLE IF EXISTS `blockchain_proof`;
CREATE TABLE `blockchain_proof` (
  `proof_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `business_key` VARCHAR(64) NOT NULL COMMENT 'qsId/eventId',
  `proof_type` VARCHAR(32) NOT NULL COMMENT 'QR_HASH/QR_FREEZE/ALERT_EVENT',
  `hash` VARCHAR(64) NOT NULL COMMENT 'sha256(payload)',
  `payload` LONGTEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`proof_id`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_business_type` (`business_key`,`proof_type`),
  KEY `idx_hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- relaxed uniqueness: allow multiple same (business_key, proof_type)
INSERT INTO `blockchain_proof` (`business_key`,`proof_type`,`hash`,`payload`) VALUES
('QS000001','QR_HASH','hash123','{"qsId":"QS000001","type":"create"}'),
('QS000001','QR_HASH','hash124','{"qsId":"QS000001","type":"recreate"}');

-- ==================================================
-- 库 6：yx_geo_profile 设备地理画像库
-- ==================================================
DROP DATABASE IF EXISTS `yx_geo_profile`;
CREATE DATABASE `yx_geo_profile` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_geo_profile`;

DROP TABLE IF EXISTS `device_profile`;
CREATE TABLE `device_profile` (
    `device_fingerprint` VARCHAR(64) PRIMARY KEY,
    `first_seen` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_seen` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `city` VARCHAR(50) NOT NULL,
    `province` VARCHAR(50) NOT NULL,
    `os` VARCHAR(20) NOT NULL,
    `browser` VARCHAR(20) NOT NULL,
    `is_risk` TINYINT(1) DEFAULT 0,
    `scan_count` INT DEFAULT 1,
    KEY `idx_device_risk` (`is_risk`),
    KEY `idx_province_city` (`province`, `city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `device_profile` (`device_fingerprint`,`first_seen`,`last_seen`,`city`,`province`,`os`,`browser`,`is_risk`,`scan_count`) VALUES
('device1',NOW(),NOW(),'攸县','湖南','Android','WeChat',0,1);

-- ==================================================
-- 库 7：yx_ai_feature AI特征库
-- ==================================================
DROP DATABASE IF EXISTS `yx_ai_feature`;
CREATE DATABASE `yx_ai_feature` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_ai_feature`;

DROP TABLE IF EXISTS `reuse_pattern`;
CREATE TABLE `reuse_pattern` (
    `pattern_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `qs_id` VARCHAR(32) NOT NULL,
    `scan_count` INT NOT NULL,
    `time_variance` DOUBLE NULL,
    `location_variance` DOUBLE NULL,
    `device_count` INT NOT NULL,
    `ip_count` INT NOT NULL,
    `is_reused` TINYINT(1) NOT NULL,
    `model_version` VARCHAR(20) NOT NULL DEFAULT 'v1.0',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_qs` (`qs_id`),
    KEY `idx_is_reused` (`is_reused`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `reuse_pattern` (`qs_id`,`scan_count`,`device_count`,`ip_count`,`is_reused`) VALUES
('QS000001',5,2,1,0);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'OK: unified multi-database schema initialized.' AS result;

