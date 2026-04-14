-- ==================================================
-- 移除 sys_notification 表的唯一约束
-- 原因：操作通知不需要唯一约束，每次操作都应该生成新通知
-- ==================================================

USE `yx_alert_engine`;

-- 删除唯一约束
ALTER TABLE `sys_notification` DROP INDEX `uk_notification_unique`;