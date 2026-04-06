-- Patch: extend scan_log schema for scan risk core fields (MySQL 5.7/8.0 compatible)
USE `yx_scan_anomaly`;

SET @db := 'yx_scan_anomaly';
SET @tbl := 'scan_log';

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='batch_id'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `batch_id` VARCHAR(32) NULL COMMENT ''关联批次ID'' AFTER `qs_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='company_id'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `company_id` VARCHAR(32) NULL COMMENT ''关联企业ID'' AFTER `batch_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='browser'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `browser` VARCHAR(20) NULL COMMENT ''设备浏览器/客户端'' AFTER `device_fingerprint`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='location_source'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `location_source` VARCHAR(32) NULL COMMENT ''定位数据来源'' AFTER `lng`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='distance_km'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `distance_km` DOUBLE NULL COMMENT ''扫码点与期望点距离(km)'' AFTER `location_source`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='new_device'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `new_device` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否新设备：1是 0否'' AFTER `is_first`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME=@tbl AND COLUMN_NAME='risk_device'),
    'SELECT 1',
    'ALTER TABLE `scan_log` ADD COLUMN `risk_device` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否风险设备：1是 0否'' AFTER `new_device`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'OK: scan_log patch applied.' AS result;
