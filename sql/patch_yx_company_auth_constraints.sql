-- Patch: harden yx_company_auth uniqueness constraints
-- Target: MySQL 5.7+/8.0+

USE `yx_company_auth`;

-- 0.1) ensure apply_by/apply_time columns exist for application audit
SET @has_apply_by_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'company_auth'
    AND column_name = 'apply_by'
);
SET @sql_add_apply_by_col := IF(
  @has_apply_by_col = 0,
  'ALTER TABLE `company_auth` ADD COLUMN `apply_by` VARCHAR(64) NULL COMMENT ''Applicant username'' AFTER `review_status`',
  'SELECT "skip add apply_by column"'
);
PREPARE stmt FROM @sql_add_apply_by_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_apply_time_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'company_auth'
    AND column_name = 'apply_time'
);
SET @sql_add_apply_time_col := IF(
  @has_apply_time_col = 0,
  'ALTER TABLE `company_auth` ADD COLUMN `apply_time` DATETIME NULL COMMENT ''Apply submit time'' AFTER `apply_by`',
  'SELECT "skip add apply_time column"'
);
PREPARE stmt FROM @sql_add_apply_time_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 0) ensure phone column exists for phone-based login
SET @has_phone_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND column_name = 'phone'
);
SET @sql_add_phone_col := IF(
  @has_phone_col = 0,
  'ALTER TABLE `auth_user` ADD COLUMN `phone` VARCHAR(20) NULL COMMENT ''Login mobile phone'' AFTER `username`',
  'SELECT "skip add phone column"'
);
PREPARE stmt FROM @sql_add_phone_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1) auth_user.username must stay unique
SET @has_uk_username := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND index_name = 'uk_auth_user_username'
);
SET @sql_uk_username := IF(
  @has_uk_username = 0,
  'ALTER TABLE `auth_user` ADD UNIQUE KEY `uk_auth_user_username` (`username`)',
  'SELECT "skip uk_auth_user_username"'
);
PREPARE stmt FROM @sql_uk_username;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.1) auth_user.phone should be unique for phone login
SET @has_uk_auth_user_phone := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND index_name = 'uk_auth_user_phone'
);
SET @sql_uk_auth_user_phone := IF(
  @has_uk_auth_user_phone = 0,
  'ALTER TABLE `auth_user` ADD UNIQUE KEY `uk_auth_user_phone` (`phone`)',
  'SELECT "skip uk_auth_user_phone"'
);
PREPARE stmt FROM @sql_uk_auth_user_phone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2) index for phone lookup
SET @has_idx_auth_user_phone := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND index_name = 'idx_auth_user_phone'
);
SET @sql_idx_auth_user_phone := IF(
  @has_idx_auth_user_phone = 0,
  'ALTER TABLE `auth_user` ADD KEY `idx_auth_user_phone` (`phone`)',
  'SELECT "skip idx_auth_user_phone"'
);
PREPARE stmt FROM @sql_idx_auth_user_phone;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) company_auth.company_id must be unique (one company one latest review row)
SET @has_uk_company_auth_company_id := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'company_auth'
    AND index_name = 'uk_company_auth_company_id'
);
SET @sql_uk_company_auth_company_id := IF(
  @has_uk_company_auth_company_id = 0,
  'ALTER TABLE `company_auth` ADD UNIQUE KEY `uk_company_auth_company_id` (`company_id`)',
  'SELECT "skip uk_company_auth_company_id"'
);
PREPARE stmt FROM @sql_uk_company_auth_company_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) auth_user.company_id unique for non-null values (MySQL allows multiple NULL)
--    Prevents two COMPANY accounts binding to the same company_id.
SET @has_uk_auth_user_company_id := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND index_name = 'uk_auth_user_company_id'
);
SET @sql_uk_auth_user_company_id := IF(
  @has_uk_auth_user_company_id = 0,
  'ALTER TABLE `auth_user` ADD UNIQUE KEY `uk_auth_user_company_id` (`company_id`)',
  'SELECT "skip uk_auth_user_company_id"'
);
PREPARE stmt FROM @sql_uk_auth_user_company_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) optional index for review queue query performance
SET @has_idx_company_auth_review_status := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'company_auth'
    AND index_name = 'idx_company_auth_review_status'
);
SET @sql_idx_company_auth_review_status := IF(
  @has_idx_company_auth_review_status = 0,
  'ALTER TABLE `company_auth` ADD KEY `idx_company_auth_review_status` (`review_status`)',
  'SELECT "skip idx_company_auth_review_status"'
);
PREPARE stmt FROM @sql_idx_company_auth_review_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'OK: yx_company_auth constraints patched.' AS result;

