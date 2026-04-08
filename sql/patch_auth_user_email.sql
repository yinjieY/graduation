-- Patch: add email support for auth_user registration and notification routing
-- Target: MySQL 5.7+/8.0+

USE `yx_company_auth`;

SET @has_email_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND column_name = 'email'
);
SET @sql_add_email_col := IF(
  @has_email_col = 0,
  'ALTER TABLE `auth_user` ADD COLUMN `email` VARCHAR(128) NULL COMMENT ''User email for notifications'' AFTER `phone`',
  'SELECT "skip add email column"'
);
PREPARE stmt FROM @sql_add_email_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `auth_user`
SET `email` = CONCAT(`username`, '@example.com')
WHERE (`email` IS NULL OR `email` = '');

SET @sql_make_email_not_null := (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = 'yx_company_auth'
        AND table_name = 'auth_user'
        AND column_name = 'email'
        AND is_nullable = 'YES'
    ),
    'ALTER TABLE `auth_user` MODIFY COLUMN `email` VARCHAR(128) NOT NULL COMMENT ''User email for notifications''',
    'SELECT "skip email not null"'
  )
);
PREPARE stmt FROM @sql_make_email_not_null;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_auth_user_email := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = 'yx_company_auth'
    AND table_name = 'auth_user'
    AND index_name = 'uk_auth_user_email'
);
SET @sql_uk_auth_user_email := IF(
  @has_uk_auth_user_email = 0,
  'ALTER TABLE `auth_user` ADD UNIQUE KEY `uk_auth_user_email` (`email`)',
  'SELECT "skip uk_auth_user_email"'
);
PREPARE stmt FROM @sql_uk_auth_user_email;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'OK: auth_user email patched.' AS result;

