-- =========================================================
-- QSGuard Auth DB Init Script
-- File: qs-auth-service/src/main/resources/sql/init_auth_schema.sql
-- Target: MySQL 5.7+ / 8.0+
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `yx_company_auth`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `yx_company_auth`;

-- Recreate tables from scratch (you said all old tables were removed;
-- this keeps the script directly reusable).
DROP TABLE IF EXISTS `company_auth_ext`;
DROP TABLE IF EXISTS `company_auth`;
DROP TABLE IF EXISTS `auth_user`;

-- -----------------------------
-- Step 1: User table for auth
-- -----------------------------
CREATE TABLE `auth_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `username` VARCHAR(64) NOT NULL COMMENT 'Login username',
  `phone` VARCHAR(20) NOT NULL COMMENT 'Login mobile phone',
  `email` VARCHAR(100) DEFAULT NULL COMMENT 'User email for notifications',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt encoded password',
  `role` VARCHAR(32) NOT NULL COMMENT 'ADMIN/COMPANY/CONSUMER/SERVICE',
  `company_id` VARCHAR(32) DEFAULT NULL COMMENT 'Bound company for COMPANY role',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_username` (`username`),
  UNIQUE KEY `uk_auth_user_phone` (`phone`),
  KEY `idx_auth_user_role` (`role`),
  KEY `idx_auth_user_company_id` (`company_id`),
  KEY `idx_auth_user_phone` (`phone`),
  KEY `idx_auth_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Authentication users';

-- --------------------------------------
-- Step 2: Company review status table
-- --------------------------------------
CREATE TABLE `company_auth` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` VARCHAR(32) NOT NULL COMMENT 'Business company id',
  `company_name` VARCHAR(128) DEFAULT NULL,
  `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '1=approved,0=rejected/pending',
  `apply_by` VARCHAR(64) DEFAULT NULL COMMENT 'Applicant username',
  `apply_time` DATETIME DEFAULT NULL COMMENT 'Apply submit time',
  `review_by` VARCHAR(64) DEFAULT NULL,
  `review_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_auth_company_id` (`company_id`),
  KEY `idx_company_auth_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Company qualification review status';

-- ------------------------------------------------------
-- Step 2.5: Company auth extended info table
-- ------------------------------------------------------
CREATE TABLE `company_auth_ext` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` VARCHAR(32) NOT NULL COMMENT 'Business company id',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '企业地址',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '企业邮箱（用于接收预警通知）',
  `lat` DOUBLE DEFAULT NULL COMMENT '企业所在纬度',
  `lng` DOUBLE DEFAULT NULL COMMENT '企业所在经度',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_auth_ext_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Company auth extended info';

-- -------------------------------------------------
-- Step 3: Seed company review data
-- -------------------------------------------------
INSERT INTO `company_auth` (`company_id`, `company_name`, `review_status`, `apply_by`, `apply_time`, `review_by`, `review_time`, `remark`)
VALUES
  ('C001', '攸县老灶香干厂', 1, 'admin001', NOW(), 'admin001', NOW(), 'Initialized as approved'),
  ('C002', '湘东豆制品有限公司', 0, 'company01', NOW(), NULL, NULL, 'Initialized as not approved');

-- -------------------------------------------------
-- Step 4: Seed demo users
-- -------------------------------------------------
-- Initial password for all users below: password
-- BCrypt hash value (password):
-- $2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa
INSERT INTO `auth_user` (`username`, `phone`, `email`, `password_hash`, `role`, `company_id`, `status`, `deleted`)
VALUES
  ('admin001',  '13800000001', 'admin@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'ADMIN',    NULL,   1, 0),
  ('company01', '13800000002', 'company01@laozao.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'COMPANY',  'C001', 1, 0),
  ('consumer01','1380000003', 'consumer01@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9hA3/5fM9vDOMkMt2rt7NmBGG99nmCa', 'CONSUMER', NULL,   1, 0);

SET FOREIGN_KEY_CHECKS = 1;

-- Quick verify:
-- SELECT id, username, role, company_id, status FROM auth_user ORDER BY id;
-- SELECT company_id, company_name, review_status FROM company_auth ORDER BY company_id;

