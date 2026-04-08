-- Patch: add consumer feedback flow table in yx_scan_anomaly
-- Target: MySQL 5.7+/8.0+

USE `yx_scan_anomaly`;

CREATE TABLE IF NOT EXISTS `feedback_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈记录自增ID',
  `feedback_id` VARCHAR(40) NOT NULL COMMENT '反馈唯一编号',
  `qs_id` VARCHAR(32) NOT NULL COMMENT '二维码ID',
  `batch_id` VARCHAR(32) NULL COMMENT '批次ID',
  `company_id` VARCHAR(32) NULL COMMENT '企业ID',
  `device_fingerprint` VARCHAR(64) NOT NULL COMMENT '设备指纹',
  `feedback_type` VARCHAR(20) NOT NULL COMMENT '反馈类型',
  `description` VARCHAR(200) NULL COMMENT '反馈描述',
  `region` VARCHAR(100) NOT NULL COMMENT '反馈地区',
  `lat` DOUBLE NULL COMMENT '纬度',
  `lng` DOUBLE NULL COMMENT '经度',
  `image_file` VARCHAR(128) NOT NULL COMMENT '水印后图片文件名',
  `qr_status` VARCHAR(20) NULL COMMENT '提交时二维码状态',
  `complaint_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '投诉率',
  `risk_level` VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT '反馈风险等级',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '反馈处理状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_feedback_id` (`feedback_id`),
  UNIQUE KEY `uk_qs_device_feedback` (`qs_id`,`device_fingerprint`),
  KEY `idx_feedback_qs_time` (`qs_id`,`created_at`),
  KEY `idx_feedback_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '消费者质量反馈表';

SELECT 'OK: feedback flow patched.' AS result;

