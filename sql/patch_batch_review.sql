-- ==================================================
-- 补丁：为 product_batch 表添加审核相关字段
-- 目的：支持批次审核流程
-- ==================================================

USE `yx_trace_core`;

-- 添加审核状态字段
ALTER TABLE `product_batch` ADD COLUMN `review_status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '审核状态：DRAFT(草稿)/PENDING(待审核)/APPROVED(已通过)/REJECTED(已拒绝)';

-- 添加审核评论字段
ALTER TABLE `product_batch` ADD COLUMN `review_comment` VARCHAR(255) DEFAULT NULL COMMENT '审核评论（拒绝原因）';

-- 添加审核时间字段
ALTER TABLE `product_batch` ADD COLUMN `review_time` DATETIME DEFAULT NULL COMMENT '审核时间';

-- 为现有数据设置默认值
UPDATE `product_batch` SET `review_status` = 'DRAFT' WHERE `review_status` IS NULL;

SELECT 'OK: product_batch review fields added successfully.' AS result;