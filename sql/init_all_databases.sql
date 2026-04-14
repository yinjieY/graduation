-- ==================================================
-- QSGuard Multi-DB Initialization Script (Unified)
-- Target: MySQL 5.7+ / 8.0+
-- Purpose:
--   1) Unify company_id rule to C001/C002 style
--   2) Provide login-ready BCrypt seed users
--   3) Relax blockchain proof uniqueness and align with code fields
--   4) Ensure comprehensive table and field annotations
--   5) Maintain data consistency across all databases
-- ==================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==================================================
-- 库 1：yx_trace_core 核心溯源库
-- 库注释：存储产品溯源核心数据，包含企业、生产批次、溯源二维码主数据
-- 用途：提供溯源数据的基础存储，支持二维码生成、批次管理等核心功能
-- ==================================================
-- 1.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_trace_core`;
-- 1.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_trace_core` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 1.3 切换到该数据库
USE `yx_trace_core`;

-- 1.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `qs_code`;
DROP TABLE IF EXISTS `product_batch`;
DROP TABLE IF EXISTS `company`;

-- 1.5 创建企业信息表
-- 用途：存储企业基本信息，作为产品批次和二维码的关联主体
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

-- 1.6 插入企业测试数据
-- 说明：初始化两个测试企业，分别为特级和一级资质
INSERT INTO `company` (`company_id`,`name`,`level`,`address`,`contact_phone`,`status`) VALUES
                                                                                           ('C001','攸县老灶香干厂','特级','湖南株洲攸县','138****1234',1),
                                                                                           ('C002','湘东豆制品有限公司','一级','湖南株洲攸县','139****5678',1);

-- 1.7 创建产品生产批次表
-- 用途：存储产品生产批次信息，关联企业信息，作为二维码生成的基础
CREATE TABLE `product_batch` (
                                 `batch_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '生产批次唯一ID',
                                 `batch_name` VARCHAR(100) NULL COMMENT '批次名称',
                                 `company_id` VARCHAR(32) NOT NULL COMMENT '所属企业ID，关联company表',
                                 `production_date` DATETIME NOT NULL COMMENT '产品实际生产日期',
                                 `ingredients` VARCHAR(200) NOT NULL COMMENT '产品配料明细',
                                 `production_standard` VARCHAR(100) NOT NULL COMMENT '产品执行标准号',
                                 `total_quantity` INT NOT NULL COMMENT '批次生产总数量',
                                 `review_status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '审核状态：DRAFT(草稿)/PENDING(待审核)/APPROVED(已通过)/REJECTED(已拒绝)',
                                 `review_comment` VARCHAR(255) DEFAULT NULL COMMENT '审核评论（拒绝原因）',
                                 `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
                                 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                 `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                 CONSTRAINT `fk_batch_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '产品生产批次表';

-- 1.8 插入批次测试数据
-- 说明：为每个企业创建一个测试批次，包含不同的配料和数量
INSERT INTO `product_batch` (`batch_id`,`batch_name`,`company_id`,`production_date`,`ingredients`,`production_standard`,`total_quantity`) VALUES
                                                                                                                                 ('BATCH2026_C001_001','2026年第一批香干','C001',NOW(),'黄豆、水','GB/T 22106-2008',500),
                                                                                                                                 ('BATCH2026_C002_001','2026年第一批麻辣香干','C002',NOW(),'黄豆、辣椒','GB/T 22106-2008',300);

-- 1.9 创建溯源二维码表
-- 用途：存储溯源二维码信息，包含二维码ID、关联批次、企业信息、签名值等
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
                           KEY `idx_qs_company` (`company_id`) COMMENT '企业ID索引，加速企业相关查询',
                           KEY `idx_qs_batch` (`batch_id`) COMMENT '批次ID索引，加速批次相关查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '溯源二维码表';

-- 1.10 插入二维码测试数据
-- 说明：为每个批次创建一个测试二维码，状态为活跃，最大扫码次数为5次
INSERT INTO `qs_code` (`qs_id`,`batch_id`,`company_id`,`qs_url`,`sm2_sign`,`issue_time`,`status`,`max_allowed_scans`) VALUES
                                                                                                                          ('QS000001','BATCH2026_C001_001','C001','http://trace.local/qs/QS000001','sign123',NOW(),'active',5),
                                                                                                                          ('QS000002','BATCH2026_C002_001','C002','http://trace.local/qs/QS000002','sign456',NOW(),'active',5);

-- ==================================================
-- 库 2：yx_scan_anomaly 扫码行为库
-- 库注释：记录用户扫码行为日志，用于异常行为分析
-- 用途：存储用户扫码行为数据，支持异常检测和风险评估
-- ==================================================
-- 2.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_scan_anomaly`;
-- 2.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_scan_anomaly` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 2.3 切换到该数据库
USE `yx_scan_anomaly`;

-- 2.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `scan_log`;
-- 2.5 创建扫码行为日志表
-- 用途：记录用户扫码行为，包含设备信息、位置信息、风险标记等
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
                            KEY `idx_qs_time` (`qs_id`, `scan_time`) COMMENT '二维码ID+扫码时间索引，加速按二维码查询历史',
                            KEY `idx_device` (`device_fingerprint`) COMMENT '设备指纹索引，加速设备相关查询',
                            KEY `idx_scan_time` (`scan_time`) COMMENT '扫码时间索引，加速时间范围查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '扫码行为日志表';

-- 2.6 插入扫码测试数据
-- 说明：为每个测试二维码创建一条扫码记录，包含不同的设备和位置信息
INSERT INTO `scan_log` (`qs_id`,`batch_id`,`company_id`,`scan_time`,`ip`,`ip_masked`,`device_fingerprint`,`browser`,`lat`,`lng`,`location_source`,`distance_km`,`is_first`,`new_device`,`risk_device`,`created_at`) VALUES
                                                                                                                                                                                                   ('QS000001','BATCH2026_C001_001','C001',NOW(),'127.0.0.1','127.0.0.*','device1','WeChat',27.24,113.77,'seed',3.2,1,1,0,NOW()),
                                                                                                                                                                                                   ('QS000002','BATCH2026_C002_001','C002',NOW(),'127.0.0.1','127.0.0.*','device2','Browser',27.86,113.13,'seed',12.8,0,1,0,NOW());

-- ==================================================
-- 表 2：feedback_record 消费者质量反馈表
-- 表注释：存储消费者提交的质量反馈信息
-- 用途：记录消费者对产品的质量反馈，支持问题追踪和处理
-- ==================================================
-- 2.7 删除旧表（如果存在）
DROP TABLE IF EXISTS `feedback_record`;
-- 2.8 创建消费者质量反馈表
CREATE TABLE `feedback_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈记录自增ID',
  `feedback_id` VARCHAR(40) NOT NULL COMMENT '反馈唯一编号(FB+时间戳+随机数)',
  `qs_id` VARCHAR(32) NOT NULL COMMENT '二维码ID',
  `batch_id` VARCHAR(32) NULL COMMENT '批次ID',
  `company_id` VARCHAR(32) NULL COMMENT '企业ID',
  `device_fingerprint` VARCHAR(64) NOT NULL COMMENT '设备指纹(用于防重复提交)',
  `submitter_ip` VARCHAR(64) NULL COMMENT '提交者IP地址',
  `feedback_type` VARCHAR(20) NOT NULL COMMENT '反馈类型(CROSS_REGION跨区销售/COUNTERFEIT假冒伪劣/OTHER其他)',
  `description` VARCHAR(200) NULL COMMENT '反馈详细描述',
  `region` VARCHAR(100) NOT NULL COMMENT '反馈地区(省/市/区)',
  `lat` DOUBLE NULL COMMENT '纬度坐标(可选)',
  `lng` DOUBLE NULL COMMENT '经度坐标(可选)',
  `image_file` VARCHAR(128) NOT NULL COMMENT '水印后图片文件名',
  `qr_status` VARCHAR(20) NULL COMMENT '提交时二维码状态(ACTIVE/INVALID/FROZEN)',
  `complaint_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '投诉率(反馈数/扫码数)',
  `risk_level` VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT '风险等级(NONE无/LOW低/MEDIUM中/HIGH高)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '处理状态(SUBMITTED已提交/ACCEPTED已受理/REJECTED已驳回/CLOSED已结案)',
  `handle_user` VARCHAR(64) NULL COMMENT '处理人(管理员账号)',
  `handle_note` VARCHAR(255) NULL COMMENT '处理备注',
  `handle_time` DATETIME NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_feedback_id` (`feedback_id`) COMMENT '反馈编号唯一索引',
  UNIQUE KEY `uk_qs_device_feedback` (`qs_id`,`device_fingerprint`) COMMENT '同一设备对同一二维码仅能提交一次',
  KEY `idx_feedback_qs_time` (`qs_id`,`created_at`) COMMENT '二维码+时间索引，加速按二维码查询反馈',
  KEY `idx_feedback_company` (`company_id`) COMMENT '企业索引，加速企业相关反馈查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费者质量反馈记录表';

-- 2.9 插入反馈测试数据
-- 说明：创建两条测试反馈记录，分别为假冒伪劣和跨区域销售类型
INSERT INTO `feedback_record` (`feedback_id`,`qs_id`,`batch_id`,`company_id`,`device_fingerprint`,`submitter_ip`,`feedback_type`,`description`,`region`,`lat`,`lng`,`image_file`,`qr_status`,`complaint_rate`,`risk_level`,`status`) VALUES
('FB20260412170000001','QS000001','BATCH2026_C001_001','C001','device1','127.0.0.1','COUNTERFEIT','疑似假冒产品','湖南省株洲市攸县',27.24,113.77,'FB20260412170000001.png','active',0.5000,'HIGH','SUBMITTED'),
('FB20260412170000002','QS000002','BATCH2026_C002_001','C002','device2','127.0.0.1','CROSS_REGION','跨区域销售','湖南省长沙市',28.22,112.93,'FB20260412170000002.png','active',0.3000,'MEDIUM','SUBMITTED');

-- ==================================================
-- 库 3：yx_alert_engine 预警引擎库
-- 库注释：预警规则、预警记录、预警执行动作管理
-- 用途：管理预警规则，记录预警事件，执行预警动作
-- ==================================================
-- 3.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_alert_engine`;
-- 3.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_alert_engine` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 3.3 切换到该数据库
USE `yx_alert_engine`;

-- 3.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `alert_action`;
DROP TABLE IF EXISTS `alert_read`;
DROP TABLE IF EXISTS `alert_record`;
DROP TABLE IF EXISTS `alert_rule`;

-- 3.5 创建预警规则配置表
-- 用途：存储预警规则配置，定义预警触发条件和等级
CREATE TABLE `alert_rule` (
                              `rule_id` VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '预警规则唯一ID',
                              `rule_name` VARCHAR(100) NOT NULL COMMENT '预警规则名称',
                              `rule_content` TEXT NOT NULL COMMENT '预警规则触发逻辑',
                              `threshold` VARCHAR(50) NOT NULL COMMENT '预警触发阈值条件',
                              `alert_level` TINYINT NOT NULL COMMENT '预警等级：1严重 2中等 3轻微',
                              `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '规则状态：1启用 0禁用',
                              `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '规则更新时间',
                              KEY `idx_rule_level` (`alert_level`) COMMENT '预警等级索引，加速按等级查询规则'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警规则配置表';

-- 3.6 插入预警规则测试数据
-- 说明：创建三条测试预警规则，分别为高频扫码、多设备扫码和多IP扫码
INSERT INTO `alert_rule` (`rule_id`,`rule_name`,`rule_content`,`threshold`,`alert_level`,`status`) VALUES
    ('R000','AI模型触发','AI模型单独检测到风险（无规则命中）','AI_ONLY',1,1),
    ('R001','高频扫码','IF scan_count_1h >= threshold THEN alert','1h>=5',2,1),
    ('R002','多设备扫码','IF device_count_1d >= threshold THEN alert','1d>=10',2,1),
    ('R003','多IP扫码','IF ip_count_1h >= threshold THEN alert','1h>=20',1,1);

-- 3.7 创建预警事件记录表
-- 用途：记录触发的预警事件，包含预警原因、处理状态等
CREATE TABLE `alert_record` (
                                `alert_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预警记录自增ID',
                                `qs_id` VARCHAR(32) NOT NULL COMMENT '触发预警的二维码ID',
                                `company_id` VARCHAR(32) NOT NULL COMMENT '关联企业ID',
                                `rule_id` VARCHAR(32) NOT NULL COMMENT '触发的预警规则ID',
                                `alert_level` TINYINT NOT NULL COMMENT '预警等级',
                                `reason` VARCHAR(200) NOT NULL COMMENT '预警简要原因',
                                `detail` TEXT NULL COMMENT '预警详细描述信息',
                                `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预警生成时间',
                                `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '预警更新时间',
                                `status` ENUM('open','closed') DEFAULT 'open' COMMENT '预警处理状态：待处理/已处理',
                                `batch_id` VARCHAR(32) NULL COMMENT '关联生产批次ID',
                                `batch_name` VARCHAR(100) NULL COMMENT '批次名称',
                                `handle_user` VARCHAR(50) NULL COMMENT '处理人账号',
                                `handle_time` DATETIME NULL COMMENT '预警处理完成时间',
                                `handle_note` VARCHAR(200) NULL COMMENT '处理备注说明',
                                KEY `idx_company_level` (`company_id`, `alert_level`) COMMENT '企业+等级索引，加速企业预警查询',
                                KEY `idx_qs` (`qs_id`) COMMENT '二维码ID索引，加速二维码预警查询',
                                KEY `idx_status` (`status`) COMMENT '状态索引，加速按状态查询',
                                KEY `idx_alert_batch` (`batch_id`) COMMENT '批次ID索引，加速批次预警查询',
                                CONSTRAINT `fk_alert_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警事件记录表';

-- 3.8 插入预警记录测试数据
-- 说明：创建一条测试预警记录，模拟高频扫码触发的预警
INSERT INTO `alert_record` (`qs_id`,`company_id`,`rule_id`,`alert_level`,`reason`,`detail`,`batch_id`,`batch_name`) VALUES
    ('QS000001','C001','R001',2,'疑似复用','扫码次数超标','BATCH2026_C001_001','2026年第一批香干');

-- 3.9 创建预警消息已读记录表
-- 用途：记录用户已读预警记录，支持已读状态管理
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

-- 3.10 创建预警执行动作表
-- 用途：记录预警触发的执行动作，包含动作类型、执行结果等
CREATE TABLE `alert_action` (
                                `action_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预警动作执行ID',
                                `alert_id` BIGINT NOT NULL COMMENT '关联预警记录ID',
                                `action_type` ENUM('freeze','notify','blockchain_record') NOT NULL COMMENT '执行动作类型：冻结/通知/上链存证',
                                `action_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '动作执行时间',
                                `result` VARCHAR(200) NOT NULL COMMENT '动作执行结果描述',
                                `push_status` ENUM('success','fail','retry') DEFAULT 'success' COMMENT '推送状态：成功/失败/重试',
                                `retry_count` TINYINT(1) DEFAULT 0 COMMENT '执行重试次数',
                                KEY `idx_alert_action` (`alert_id`) COMMENT '预警ID索引，加速按预警查询动作',
                                CONSTRAINT `fk_action_alert` FOREIGN KEY (`alert_id`) REFERENCES `alert_record` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '预警执行动作表';

-- 3.11 插入预警动作测试数据
-- 说明：为测试预警记录创建一条通知动作记录
INSERT INTO `alert_action` (`alert_id`,`action_type`,`result`) VALUES
    (1,'notify','发送预警成功');

-- 3.12 创建系统通知表
-- 用途：存储系统公告、审核结果、操作日志等通知信息
CREATE TABLE `sys_notification` (
    `notification_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知记录自增ID',
    `company_id` VARCHAR(32) NULL COMMENT '关联企业ID（为空表示系统公告）',
    `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `type` VARCHAR(20) NOT NULL COMMENT '通知类型：SYSTEM(系统公告)/REVIEW(审核结果)/OPERATION(操作日志)',
    `status` VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '通知状态：UNREAD/READ(仅定向消息使用)',
    `source_module` VARCHAR(50) NULL COMMENT '来源模块',
    `source_id` VARCHAR(64) NULL COMMENT '来源业务ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
    `read_time` DATETIME NULL COMMENT '阅读时间(仅定向消息使用)',
    KEY `idx_company_id` (`company_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    UNIQUE KEY `uk_notification_unique` (`company_id`, `title`(50), `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- 3.13 创建系统公告已读记录表
-- 用途：记录企业对系统公告的阅读状态（系统公告所有企业共享，需单独记录阅读状态）
CREATE TABLE `notification_read` (
    `read_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '已读记录自增ID',
    `notification_id` BIGINT NOT NULL COMMENT '关联通知ID',
    `company_id` VARCHAR(32) NOT NULL COMMENT '企业ID',
    `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'READ' COMMENT '阅读状态',
    KEY `idx_notification_id` (`notification_id`),
    KEY `idx_company_id` (`company_id`),
    UNIQUE KEY `uk_notification_company` (`notification_id`, `company_id`),
    CONSTRAINT `fk_read_notification` FOREIGN KEY (`notification_id`) REFERENCES `sys_notification`(`notification_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告已读记录表';

-- 3.14 插入系统通知测试数据
INSERT INTO `sys_notification` (`company_id`, `title`, `content`, `type`, `source_module`, `source_id`) VALUES
    ('C001', '批次审核结果通知', '您的批次【2026年第一批香干】已通过审核', 'REVIEW', 'BATCH', 'BATCH2026_C001_001'),
    ('C001', '系统维护通知', '系统将于今晚22:00-24:00进行例行维护，届时可能影响部分功能', 'SYSTEM', 'SYSTEM', 'INIT_001'),
    ('C002', '企业资质审核提醒', '您的企业资质审核正在处理中，请耐心等待', 'REVIEW', 'COMPANY_AUTH', 'C002'),
    (NULL, '系统公告', '尊敬的用户，QSGuard溯源系统已正式上线！', 'SYSTEM', 'SYSTEM', 'INIT_002');

-- ==================================================
-- 库 4：yx_company_auth 企业认证与登录库
-- 库注释：系统用户认证、权限、企业资质审核管理
-- 用途：管理系统用户账号、权限和企业资质审核
-- ==================================================
-- 4.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_company_auth`;
-- 4.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_company_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 4.3 切换到该数据库
USE `yx_company_auth`;

-- 4.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `company_auth`;
DROP TABLE IF EXISTS `auth_user`;

-- 4.5 创建系统用户表
-- 用途：存储系统用户账号信息，包含登录凭证、角色权限等
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
                             PRIMARY KEY (`id`) COMMENT '用户ID主键',
                             UNIQUE KEY `uk_auth_user_username` (`username`) COMMENT '用户名唯一索引',
                             UNIQUE KEY `uk_auth_user_phone` (`phone`) COMMENT '手机号唯一索引',
                             KEY `idx_auth_user_role` (`role`) COMMENT '角色索引，加速按角色查询',
                             KEY `idx_auth_user_company_id` (`company_id`) COMMENT '企业ID索引，加速企业用户查询',
                             KEY `idx_auth_user_phone` (`phone`) COMMENT '手机号索引，加速手机号查询',
                             KEY `idx_auth_user_status` (`status`) COMMENT '状态索引，加速按状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统用户表';

-- 4.6 创建企业资质认证审核表
-- 用途：存储企业资质认证申请和审核记录
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
                                PRIMARY KEY (`id`) COMMENT '认证记录ID主键',
                                UNIQUE KEY `uk_company_auth_company_id` (`company_id`) COMMENT '企业ID唯一索引',
                                KEY `idx_company_auth_review_status` (`review_status`) COMMENT '审核状态索引，加速按状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '企业资质认证审核表';

-- 4.7 插入企业认证测试数据
-- 说明：创建两条测试企业认证记录，一条已通过，一条待审核
INSERT INTO `company_auth` (`company_id`,`company_name`,`review_status`,`apply_by`,`apply_time`,`review_by`,`review_time`,`remark`) VALUES
                                                                                                                ('C001','攸县老灶香干厂',1,'admin001',NOW(),'admin001',NOW(),'Initialized as approved'),
                                                                                                                ('C002','湘东豆制品有限公司',0,'company01',NOW(),NULL,NULL,'Initialized as not approved');

-- 4.8 插入系统用户测试数据
-- 说明：创建三个测试用户，分别为管理员、企业用户和消费者用户
-- initial password: password
-- bcrypt: $2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa
INSERT INTO `auth_user` (`username`,`phone`,`password_hash`,`role`,`company_id`,`status`,`deleted`) VALUES
                                                                                                ('admin001',  '13800000001', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'ADMIN', NULL,   1, 0),
                                                                                                ('company01', '13800000002', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'COMPANY','C001', 1, 0),
                                                                                                ('consumer01','13800000003', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'CONSUMER',NULL,  1, 0);

-- ==================================================
-- 库 5：yx_blockchain_proof 区块链存证库
-- 库注释：溯源数据区块链存证，保证数据不可篡改
-- 用途：存储区块链存证记录，确保数据的完整性和不可篡改性
-- ==================================================
-- 5.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_blockchain_proof`;
-- 5.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_blockchain_proof` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 5.3 切换到该数据库
USE `yx_blockchain_proof`;

-- 5.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `proof_dead_letter`;
DROP TABLE IF EXISTS `proof_outbox`;
DROP TABLE IF EXISTS `blockchain_proof`;
-- 5.5 创建区块链存证记录表
-- 用途：存储区块链存证记录，包含业务数据哈希、链上状态等
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
                                    PRIMARY KEY (`proof_id`) COMMENT '存证记录ID主键',
                                    UNIQUE KEY `uk_idempotency` (`idempotency_key`) COMMENT '幂等键唯一索引',
                                    KEY `idx_business_key` (`business_key`) COMMENT '业务键索引，加速业务相关查询',
                                    KEY `idx_business_type` (`business_key`,`proof_type`) COMMENT '业务键+类型索引，加速组合查询',
                                    KEY `idx_hash` (`hash`) COMMENT '哈希值索引，加速哈希查询',
                                    KEY `idx_chain_status` (`chain_status`) COMMENT '链上状态索引，加速按状态查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '区块链存证记录表';

-- 5.6 插入区块链存证测试数据
-- 说明：为测试二维码创建两条存证记录，测试多次存证场景
-- relaxed uniqueness: allow multiple same (business_key, proof_type)
INSERT INTO `blockchain_proof` (`business_key`,`proof_type`,`hash`,`payload`) VALUES
                                                                                  ('QS000001','QR_HASH','hash123','{"qsId":"QS000001","type":"create"}'),
                                                                                  ('QS000001','QR_HASH','hash124','{"qsId":"QS000001","type":"recreate"}');

-- 5.7 生成幂等键
-- 说明：为存证记录生成幂等键，确保存证操作的幂等性
UPDATE `blockchain_proof`
SET `idempotency_key` = SHA2(CONCAT(`business_key`, '|', `proof_type`, '|', `hash`), 256)
WHERE `idempotency_key` IS NULL;

-- 5.8 创建异步存证Outbox表
-- 用途：存储待异步处理的存证任务，支持重试机制
CREATE TABLE `proof_outbox` (
                                 `outbox_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Outbox记录自增ID',
                                 `proof_id` BIGINT UNSIGNED NOT NULL COMMENT '关联存证记录ID',
                                 `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RETRY/SUCCESS/DEAD',
                                 `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
                                 `next_retry_at` DATETIME NOT NULL COMMENT '下次重试时间',
                                 `last_error` VARCHAR(512) NULL COMMENT '最后一次错误信息',
                                 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`outbox_id`) COMMENT 'Outbox记录ID主键',
                                 KEY `idx_outbox_status_retry` (`status`, `next_retry_at`) COMMENT '状态+重试时间索引，加速任务调度',
                                 KEY `idx_outbox_proof` (`proof_id`) COMMENT '存证ID索引，加速关联查询',
                                 CONSTRAINT `fk_outbox_proof` FOREIGN KEY (`proof_id`) REFERENCES `blockchain_proof` (`proof_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步存证Outbox';

-- 5.9 创建存证死信记录表
-- 用途：存储处理失败的存证任务，便于后续分析和处理
CREATE TABLE `proof_dead_letter` (
                                      `dead_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '死信记录自增ID',
                                      `outbox_id` BIGINT UNSIGNED NOT NULL COMMENT '关联Outbox记录ID',
                                      `reason` VARCHAR(512) NOT NULL COMMENT '失败原因',
                                      `payload_snapshot` LONGTEXT NULL COMMENT '存证数据快照',
                                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`dead_id`) COMMENT '死信记录ID主键',
                                      KEY `idx_dead_outbox` (`outbox_id`) COMMENT 'Outbox ID索引，加速关联查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存证死信记录';

-- ==================================================
-- 库 6：yx_geo_profile 设备地理画像库
-- 库注释：设备地理信息、行为画像、风险标记
-- 用途：存储设备地理信息和行为画像，支持风险评估
-- ==================================================
-- 6.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_geo_profile`;
-- 6.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_geo_profile` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 6.3 切换到该数据库
USE `yx_geo_profile`;

-- 6.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `device_profile`;
-- 6.5 创建设备画像信息表
-- 用途：存储设备的地理信息、行为特征和风险标记
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
                                  KEY `idx_device_risk` (`is_risk`) COMMENT '风险标记索引，加速风险设备查询',
                                  KEY `idx_province_city` (`province`, `city`) COMMENT '省市索引，加速地理相关查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '设备画像信息表';

-- 6.6 插入设备画像测试数据
-- 说明：创建一条测试设备画像记录，模拟Android设备在湖南攸县的扫码行为
INSERT INTO `device_profile` (`device_fingerprint`,`first_seen`,`last_seen`,`city`,`province`,`os`,`browser`,`is_risk`,`scan_count`) VALUES
    ('device1',NOW(),NOW(),'攸县','湖南','Android','WeChat',0,1);

-- ==================================================
-- 库 7：yx_ai_feature AI特征库
-- 库注释：AI模型识别二维码复用、异常行为特征数据
-- 用途：存储AI模型识别的二维码复用特征和异常行为数据
-- ==================================================
-- 7.1 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `yx_ai_feature`;
-- 7.2 创建新数据库，设置字符集为utf8mb4
CREATE DATABASE `yx_ai_feature` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 7.3 切换到该数据库
USE `yx_ai_feature`;

-- 7.4 删除旧表（如果存在）
DROP TABLE IF EXISTS `reuse_pattern`;
-- 7.5 创建二维码复用识别特征表
-- 用途：存储二维码复用识别的特征数据，用于AI模型分析
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
                                 UNIQUE KEY `uk_qs` (`qs_id`) COMMENT '二维码ID唯一索引',
                                 KEY `idx_is_reused` (`is_reused`) COMMENT '复用标记索引，加速复用分析'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '二维码复用识别特征表';

-- 7.6 插入复用特征测试数据
-- 说明：为测试二维码创建一条复用特征记录，模拟正常使用场景
INSERT INTO `reuse_pattern` (`qs_id`,`scan_count`,`device_count`,`ip_count`,`is_reused`) VALUES
    ('QS000001',5,2,1,0);

-- 7.7 恢复外键约束
SET FOREIGN_KEY_CHECKS = 1;

-- 7.8 输出初始化完成信息
SELECT 'OK: unified multi-database schema initialized.' AS result;

