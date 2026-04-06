-- Add enterprise geo baseline table used by qs-scan-service distance evaluation.
USE `yx_geo_profile`;

CREATE TABLE IF NOT EXISTS `company_geo` (
  `geo_id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '企业地理基线主键',
  `company_id` VARCHAR(32) NOT NULL COMMENT '企业ID',
  `expected_lat` DOUBLE NOT NULL COMMENT '企业期望纬度',
  `expected_lng` DOUBLE NOT NULL COMMENT '企业期望经度',
  `address_snapshot` VARCHAR(255) NULL COMMENT '企业地址快照',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_company_geo_company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '企业扫码地理基线';

-- Update these values with your real factory/warehouse coordinates.
INSERT INTO `company_geo` (`company_id`, `expected_lat`, `expected_lng`, `address_snapshot`)
VALUES
  ('C001', 27.8015, 113.3458, '湖南株洲攸县'),
  ('C002', 27.8015, 113.3458, '湖南株洲攸县')
ON DUPLICATE KEY UPDATE
  `expected_lat` = VALUES(`expected_lat`),
  `expected_lng` = VALUES(`expected_lng`),
  `address_snapshot` = VALUES(`address_snapshot`);

