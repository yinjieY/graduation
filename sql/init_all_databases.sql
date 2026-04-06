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
-- 库注释：存储产品溯源核心数据，包含企业、生产批次、溯源二维码主数据
-- ==================================================
DROP DATABASE IF EXISTS `yx_trace_core`;
CREATE DATABASE `yx_trace_core` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_trace_core`;

DROP TABLE IF EXISTS `qs_code`;
DROP TABLE IF EXISTS `product_batch`;
DROP TABLE IF EXISTS `company`;

CREATE TABLE `company` (
                           `company_id` VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '企业唯一ID，格式C001/C002',
                           `name` VARCHAR(100) NOT NULL COMMENT '企业全称',
                           `level` ENUM('特级','一级') DEFAULT '一级' COMMENT '企业资质等级',
                           `address` VARCHAR(255) NOT NULL COMMENT '企业生产经营地址',
                           `contact_phone` VARCHAR(20) NOT NULL COMMENT '企业联系电话（已脱敏）',
                           `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '企业状态：1正常 0禁用',
                           `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                           `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '企业信息表';

INSERT INTO `company` (`company_id`,`name`,`level`,`address`,`contact_phone`,`status`) VALUES
                                                                                           ('C001','攸县老灶香干厂','特级','湖南株洲攸县','138****1234',1),
                                                                                           ('C002','湘东豆制品有限公司','一级','湖南株洲攸县','139****5678',1);

CREATE TABLE `product_batch` (
                                 `batch_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '生产批次唯一ID',
                                 `company_id` VARCHAR(32) NOT NULL COMMENT '所属企业ID，关联company表',
                                 `production_date` DATETIME NOT NULL COMMENT '产品实际生产日期',
                                 `ingredients` VARCHAR(200) NOT NULL COMMENT '产品配料明细',
                                 `production_standard` VARCHAR(100) NOT NULL COMMENT '产品执行标准号',
                                 `total_quantity` INT NOT NULL COMMENT '批次生产总数量',
                                 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                 `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                 CONSTRAINT `fk_batch_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '产品生产批次表';

INSERT INTO `product_batch` (`batch_id`,`company_id`,`production_date`,`ingredients`,`production_standard`,`total_quantity`) VALUES
                                                                                                                                 ('BATCH2026_C001_001','C001',NOW(),'黄豆、水','GB/T 22106-2008',500),
                                                                                                                                 ('BATCH2026_C002_001','C002',NOW(),'黄豆、辣椒','GB/T 22106-2008',300);

CREATE TABLE `qs_code` (
                           `qs_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '溯源二维码唯一ID',
                           `batch_id` VARCHAR(32) NOT NULL COMMENT '关联生产批次ID',
                           `company_id` VARCHAR(32) NOT NULL COMMENT '所属企业ID',
                           `qs_url` VARCHAR(1024) NOT NULL COMMENT '二维码扫码访问地址',
                           `sm2_sign` VARCHAR(256) NOT NULL COMMENT 'SM2国密算法签名值',
                           `issue_time` DATETIME NOT NULL COMMENT '二维码发放生成时间',
                           `status` ENUM('active','invalid','frozen','cancelled') DEFAULT 'active' COMMENT '二维码状态：有效/无效/冻结/注销',
                           `max_allowed_scans` INT NOT NULL DEFAULT 5 COMMENT '最大允许扫码次数',
                           `freeze_time` DATETIME NULL COMMENT '二维码冻结时间',
                           `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                           `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                           CONSTRAINT `fk_qs_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`),
                           CONSTRAINT `fk_qs_batch` FOREIGN KEY (`batch_id`) REFERENCES `product_batch` (`batch_id`),
                           KEY `idx_qs_company` (`company_id`),
                           KEY `idx_qs_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '溯源二维码表';

INSERT INTO `qs_code` (`qs_id`,`batch_id`,`company_id`,`qs_url`,`sm2_sign`,`issue_time`,`status`,`max_allowed_scans`) VALUES
                                                                                                                          ('QS000001','BATCH2026_C001_001','C001','http://trace.local/qs/QS000001','sign123',NOW(),'active',5),
                                                                                                                          ('QS000002','BATCH2026_C002_001','C002','http://trace.local/qs/QS000002','sign456',NOW(),'active',5);

-- ==================================================
-- 库 2：yx_scan_anomaly 扫码行为库
-- 库注释：记录用户扫码行为日志，用于异常行为分析
-- ==================================================
DROP DATABASE IF EXISTS `yx_scan_anomaly`;
CREATE DATABASE `yx_scan_anomaly` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_scan_anomaly`;

DROP TABLE IF EXISTS `scan_log`;
CREATE TABLE `scan_log` (
                            `scan_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '扫码记录自增ID',
                            `qs_id` VARCHAR(32) NOT NULL COMMENT '被扫描的二维码ID',
                            `batch_id` VARCHAR(32) NULL COMMENT '关联批次ID',
                            `company_id` VARCHAR(32) NULL COMMENT '关联企业ID',
                            `scan_time` DATETIME NOT NULL COMMENT '扫码发生时间',
                            `ip` VARCHAR(45) NOT NULL COMMENT '扫码设备真实IP地址',
                            `ip_masked` VARCHAR(45) NOT NULL COMMENT '脱敏后的IP地址',
                            `device_fingerprint` VARCHAR(64) NOT NULL COMMENT '设备唯一指纹标识',
                            `browser` VARCHAR(20) NULL COMMENT '设备浏览器/客户端',
                            `lat` DOUBLE NULL COMMENT '扫码位置纬度坐标',
                            `lng` DOUBLE NULL COMMENT '扫码位置经度坐标',
                            `location_source` VARCHAR(32) NULL COMMENT '定位数据来源',
                            `distance_km` DOUBLE NULL COMMENT '扫码点与期望点距离(km)',
                            `is_first` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首次扫码：1是 0否',
                            `new_device` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否新设备：1是 0否',
                            `risk_device` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否风险设备：1是 0否',
                            `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录入库时间',
                            KEY `idx_qs_time` (`qs_id`, `scan_time`),
                            KEY `idx_device` (`device_fingerprint`),
                            KEY `idx_scan_time` (`scan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '扫码行为日志表';

INSERT INTO `scan_log` (`qs_id`,`batch_id`,`company_id`,`scan_time`,`ip`,`ip_masked`,`device_fingerprint`,`browser`,`lat`,`lng`,`location_source`,`distance_km`,`is_first`,`new_device`,`risk_device`,`created_at`) VALUES
                                                                                                                                                                                                   ('QS000001','BATCH2026_C001_001','C001',NOW(),'127.0.0.1','127.0.0.*','device1','WeChat',27.24,113.77,'seed',3.2,1,1,0,NOW()),
                                                                                                                                                                                                   ('QS000002','BATCH2026_C002_001','C002',NOW(),'127.0.0.1','127.0.0.*','device2','Browser',27.86,113.13,'seed',12.8,0,1,0,NOW());

-- ==================================================
-- 库 3：yx_alert_engine 预警引擎库
-- 库注释：预警规则、预警记录、预警执行动作管理
-- ==================================================
DROP DATABASE IF EXISTS `yx_alert_engine`;
CREATE DATABASE `yx_alert_engine` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_alert_engine`;

DROP TABLE IF EXISTS `alert_action`;
DROP TABLE IF EXISTS `alert_record`;
DROP TABLE IF EXISTS `alert_rule`;

CREATE TABLE `alert_rule` (
                              `rule_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '预警规则唯一ID',
                              `rule_name` VARCHAR(100) NOT NULL COMMENT '预警规则名称',
                              `rule_content` TEXT NOT NULL COMMENT '预警规则触发逻辑',
                              `threshold` VARCHAR(50) NOT NULL COMMENT '预警触发阈值条件',
                              `alert_level` TINYINT NOT NULL COMMENT '预警等级：1严重 2中等 3轻微',
                              `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '规则状态：1启用 0禁用',
                              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '规则更新时间',
                              KEY `idx_rule_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警规则配置表';

INSERT INTO `alert_rule` (`rule_id`,`rule_name`,`rule_content`,`threshold`,`alert_level`,`status`) VALUES
    ('R001','高频扫码','IF scan_count_1h >= threshold THEN alert','1h>=5',2,1),
    ('R002','多设备扫码','IF device_count_1d >= threshold THEN alert','1d>=10',2,1),
    ('R003','多IP扫码','IF ip_count_1h >= threshold THEN alert','1h>=20',1,1);

CREATE TABLE `alert_record` (
                                `alert_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预警记录自增ID',
                                `qs_id` VARCHAR(32) NOT NULL COMMENT '触发预警的二维码ID',
                                `company_id` VARCHAR(32) NOT NULL COMMENT '关联企业ID',
                                `rule_id` VARCHAR(32) NOT NULL COMMENT '触发的预警规则ID',
                                `alert_level` TINYINT NOT NULL COMMENT '预警等级',
                                `reason` VARCHAR(200) NOT NULL COMMENT '预警简要原因',
                                `detail` TEXT NULL COMMENT '预警详细描述信息',
                                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预警生成时间',
                                `status` ENUM('open','closed') DEFAULT 'open' COMMENT '预警处理状态：待处理/已处理',
                                `handle_user` VARCHAR(50) NULL COMMENT '处理人账号',
                                `handle_time` DATETIME NULL COMMENT '预警处理完成时间',
                                `handle_note` VARCHAR(200) NULL COMMENT '处理备注说明',
                                KEY `idx_company_level` (`company_id`, `alert_level`),
                                KEY `idx_qs` (`qs_id`),
                                KEY `idx_status` (`status`),
                                CONSTRAINT `fk_alert_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警事件记录表';

INSERT INTO `alert_record` (`qs_id`,`company_id`,`rule_id`,`alert_level`,`reason`,`detail`) VALUES
    ('QS000001','C001','R001',2,'疑似复用','扫码次数超标');

CREATE TABLE `alert_action` (
                                `action_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预警动作执行ID',
                                `alert_id` BIGINT NOT NULL COMMENT '关联预警记录ID',
                                `action_type` ENUM('freeze','notify','blockchain_record') NOT NULL COMMENT '执行动作类型：冻结/通知/上链存证',
                                `action_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '动作执行时间',
                                `result` VARCHAR(200) NOT NULL COMMENT '动作执行结果描述',
                                `push_status` ENUM('success','fail','retry') DEFAULT 'success' COMMENT '推送状态：成功/失败/重试',
                                `retry_count` TINYINT(1) DEFAULT 0 COMMENT '执行重试次数',
                                KEY `idx_alert_action` (`alert_id`),
                                CONSTRAINT `fk_action_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_record` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警执行动作表';

INSERT INTO `alert_action` (`alert_id`,`action_type`,`result`) VALUES
    (1,'notify','发送预警成功');

-- ==================================================
-- 库 4：yx_company_auth 企业认证与登录库
-- 库注释：系统用户认证、权限、企业资质审核管理
-- ==================================================
DROP DATABASE IF EXISTS `yx_company_auth`;
CREATE DATABASE `yx_company_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yx_company_auth`;

DROP TABLE IF EXISTS `company_auth`;
DROP TABLE IF EXISTS `auth_user`;

CREATE TABLE `auth_user` (
                             `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户自增ID',
                             `username` VARCHAR(64) NOT NULL COMMENT '登录用户名（唯一）',
                             `phone` VARCHAR(20) NOT NULL COMMENT '登录手机号（唯一）',
                             `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码哈希',
                             `role` VARCHAR(32) NOT NULL COMMENT '用户角色：ADMIN/COMPANY/CONSUMER/SERVICE',
                             `company_id` VARCHAR(32) DEFAULT NULL COMMENT '关联企业ID（企业用户）',
                             `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1正常 0禁用',
                             `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
                             `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
                             `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账号更新时间',
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_auth_user_username` (`username`),
                             UNIQUE KEY `uk_auth_user_phone` (`phone`),
                             KEY `idx_auth_user_role` (`role`),
                             KEY `idx_auth_user_company_id` (`company_id`),
                             KEY `idx_auth_user_phone` (`phone`),
                             KEY `idx_auth_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统用户表';

CREATE TABLE `company_auth` (
                                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '认证记录自增ID',
                                `company_id` VARCHAR(32) NOT NULL COMMENT '企业唯一ID',
                                `company_name` VARCHAR(128) DEFAULT NULL COMMENT '企业名称',
                                `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1已通过 2已拒绝',
                                `apply_by` VARCHAR(64) DEFAULT NULL COMMENT '申请人账号',
                                `apply_time` DATETIME DEFAULT NULL COMMENT '申请提交时间',
                                `review_by` VARCHAR(64) DEFAULT NULL COMMENT '审核人账号',
                                `review_time` DATETIME DEFAULT NULL COMMENT '审核完成时间',
                                `remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
                                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_company_auth_company_id` (`company_id`),
                                KEY `idx_company_auth_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '企业资质认证审核表';

INSERT INTO `company_auth` (`company_id`,`company_name`,`review_status`,`apply_by`,`apply_time`,`review_by`,`review_time`,`remark`) VALUES
                                                                                                                ('C001','攸县老灶香干厂',1,'admin001',NOW(),'admin001',NOW(),'Initialized as approved'),
                                                                                                                ('C002','湘东豆制品有限公司',0,'company01',NOW(),NULL,NULL,'Initialized as not approved');

-- initial password: password
-- bcrypt: $2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa
INSERT INTO `auth_user` (`username`,`phone`,`password_hash`,`role`,`company_id`,`status`,`deleted`) VALUES
                                                                                                ('admin001',  '13800000001', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'ADMIN', NULL,   1, 0),
                                                                                                ('company01', '13800000002', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'COMPANY','C001', 1, 0),
                                                                                                ('consumer01','13800000003', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'CONSUMER',NULL,  1, 0);

-- ==================================================
-- 库 5：yx_blockchain_proof 区块链存证库
-- 库注释：溯源数据区块链存证，保证数据不可篡改
-- ==================================================
DROP DATABASE IF EXISTS `yx_blockchain_proof`;
CREATE DATABASE `yx_blockchain_proof` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `yx_blockchain_proof`;

DROP TABLE IF EXISTS `proof_dead_letter`;
DROP TABLE IF EXISTS `proof_outbox`;
DROP TABLE IF EXISTS `blockchain_proof`;
CREATE TABLE `blockchain_proof` (
                                    `proof_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '存证记录自增ID',
                                    `business_key` VARCHAR(64) NOT NULL COMMENT '业务唯一标识：qsId/eventId',
                                    `proof_type` VARCHAR(32) NOT NULL COMMENT '存证类型：QR_HASH/QR_FREEZE/ALERT_EVENT',
                                    `hash` VARCHAR(64) NOT NULL COMMENT '业务数据SHA256哈希值',
                                    `payload` LONGTEXT NOT NULL COMMENT '存证原始业务数据JSON',
                                    `chain_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
                                    `tx_hash` VARCHAR(128) NULL COMMENT '链上交易哈希',
                                    `block_number` BIGINT NULL COMMENT '链上区块高度',
                                    `contract_address` VARCHAR(128) NULL COMMENT '链上合约地址',
                                    `chain_error` VARCHAR(512) NULL COMMENT '最后一次链写错误',
                                    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
                                    `idempotency_key` VARCHAR(64) NULL COMMENT '幂等键',
                                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '存证生成时间',
                                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`proof_id`),
                                    UNIQUE KEY `uk_idempotency` (`idempotency_key`),
                                    KEY `idx_business_key` (`business_key`),
                                    KEY `idx_business_type` (`business_key`,`proof_type`),
                                    KEY `idx_hash` (`hash`),
                                    KEY `idx_chain_status` (`chain_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '区块链存证记录表';

-- relaxed uniqueness: allow multiple same (business_key, proof_type)
INSERT INTO `blockchain_proof` (`business_key`,`proof_type`,`hash`,`payload`) VALUES
                                                                                  ('QS000001','QR_HASH','hash123','{"qsId":"QS000001","type":"create"}'),
                                                                                  ('QS000001','QR_HASH','hash124','{"qsId":"QS000001","type":"recreate"}');

UPDATE `blockchain_proof`
SET `idempotency_key` = SHA2(CONCAT(`business_key`, '|', `proof_type`, '|', `hash`), 256)
WHERE `idempotency_key` IS NULL;

CREATE TABLE `proof_outbox` (
                                 `outbox_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                 `proof_id` BIGINT UNSIGNED NOT NULL,
                                 `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RETRY/SUCCESS/DEAD',
                                 `retry_count` INT NOT NULL DEFAULT 0,
                                 `next_retry_at` DATETIME NOT NULL,
                                 `last_error` VARCHAR(512) NULL,
                                 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`outbox_id`),
                                 KEY `idx_outbox_status_retry` (`status`, `next_retry_at`),
                                 KEY `idx_outbox_proof` (`proof_id`),
                                 CONSTRAINT `fk_outbox_proof` FOREIGN KEY (`proof_id`) REFERENCES `blockchain_proof` (`proof_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步存证Outbox';

CREATE TABLE `proof_dead_letter` (
                                      `dead_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                      `outbox_id` BIGINT UNSIGNED NOT NULL,
                                      `reason` VARCHAR(512) NOT NULL,
                                      `payload_snapshot` LONGTEXT NULL,
                                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`dead_id`),
                                      KEY `idx_dead_outbox` (`outbox_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存证死信记录';

-- ==================================================
-- 库 6：yx_geo_profile 设备地理画像库
-- 库注释：设备地理信息、行为画像、风险标记
-- ==================================================
DROP DATABASE IF EXISTS `yx_geo_profile`;
CREATE DATABASE `yx_geo_profile` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_geo_profile`;

DROP TABLE IF EXISTS `device_profile`;
CREATE TABLE `device_profile` (
                                  `device_fingerprint` VARCHAR(64) PRIMARY KEY COMMENT '设备唯一指纹标识',
                                  `first_seen` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '设备首次出现时间',
                                  `last_seen` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '设备最后活跃时间',
                                  `city` VARCHAR(50) NOT NULL COMMENT '设备常用城市',
                                  `province` VARCHAR(50) NOT NULL COMMENT '设备常用省份',
                                  `os` VARCHAR(20) NOT NULL COMMENT '设备操作系统类型',
                                  `browser` VARCHAR(20) NOT NULL COMMENT '设备浏览器/客户端类型',
                                  `is_risk` TINYINT(1) DEFAULT 0 COMMENT '风险设备标记：0否 1是',
                                  `scan_count` INT DEFAULT 1 COMMENT '设备累计扫码次数',
                                  KEY `idx_device_risk` (`is_risk`),
                                  KEY `idx_province_city` (`province`, `city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '设备画像信息表';

INSERT INTO `device_profile` (`device_fingerprint`,`first_seen`,`last_seen`,`city`,`province`,`os`,`browser`,`is_risk`,`scan_count`) VALUES
    ('device1',NOW(),NOW(),'攸县','湖南','Android','WeChat',0,1);

-- ==================================================
-- 库 7：yx_ai_feature AI特征库
-- 库注释：AI模型识别二维码复用、异常行为特征数据
-- ==================================================
DROP DATABASE IF EXISTS `yx_ai_feature`;
CREATE DATABASE `yx_ai_feature` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yx_ai_feature`;

DROP TABLE IF EXISTS `reuse_pattern`;
CREATE TABLE `reuse_pattern` (
                                 `pattern_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '特征记录自增ID',
                                 `qs_id` VARCHAR(32) NOT NULL COMMENT '溯源二维码ID',
                                 `scan_count` INT NOT NULL COMMENT '累计扫码总次数',
                                 `time_variance` DOUBLE NULL COMMENT '扫码时间分布方差',
                                 `location_variance` DOUBLE NULL COMMENT '扫码位置分布方差',
                                 `device_count` INT NOT NULL COMMENT '扫码设备数量',
                                 `ip_count` INT NOT NULL COMMENT '扫码IP数量',
                                 `is_reused` TINYINT(1) NOT NULL COMMENT '是否判定为复用：0否 1是',
                                 `model_version` VARCHAR(20) NOT NULL DEFAULT 'v1.0' COMMENT '使用的AI模型版本',
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '特征生成时间',
                                 `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '特征更新时间',
                                 UNIQUE KEY `uk_qs` (`qs_id`),
                                 KEY `idx_is_reused` (`is_reused`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '二维码复用识别特征表';

INSERT INTO `reuse_pattern` (`qs_id`,`scan_count`,`device_count`,`ip_count`,`is_reused`) VALUES
    ('QS000001',5,2,1,0);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'OK: unified multi-database schema initialized.' AS result;

