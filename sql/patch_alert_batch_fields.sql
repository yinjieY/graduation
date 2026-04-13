USE `yx_alert_engine`;

ALTER TABLE `alert_record` 
ADD COLUMN `batch_id` VARCHAR(32) NULL COMMENT '关联生产批次ID' AFTER `status`,
ADD COLUMN `batch_name` VARCHAR(100) NULL COMMENT '批次名称' AFTER `batch_id`,
ADD KEY `idx_alert_batch` (`batch_id`);
