-- 为 product_batch 表添加 batch_name 字段
USE yx_trace_core;

-- 检查字段是否存在，如果不存在则添加
ALTER TABLE product_batch 
ADD COLUMN IF NOT EXISTS `batch_name` VARCHAR(100) NULL COMMENT '批次名称' AFTER `batch_id`;

-- 更新现有数据的批次名称
UPDATE product_batch 
SET batch_name = CONCAT(YEAR(production_date), '年批次', batch_id) 
WHERE batch_name IS NULL OR batch_name = '';

-- 更新测试数据的批次名称
UPDATE product_batch 
SET batch_name = '2026年第一批香干' 
WHERE batch_id = 'BATCH2026_C001_001';

UPDATE product_batch 
SET batch_name = '2026年第一批麻辣香干' 
WHERE batch_id = 'BATCH2026_C002_001';

SELECT 'patch_add_batch_name.sql 执行完成' AS result;
