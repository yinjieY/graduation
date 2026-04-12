-- Patch: add consumer feedback flow table in yx_scan_anomaly
-- Target: MySQL 5.7+/8.0+

USE `yx_scan_anomaly`;

CREATE TABLE IF NOT EXISTS `feedback_record` (
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
  KEY `idx_feedback_qs_time` (`qs_id`,`created_at`) COMMENT '二维码+时间索引',
  KEY `idx_feedback_company` (`company_id`) COMMENT '企业索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费者质量反馈记录表';

SELECT 'OK: feedback flow patched.' AS result;

