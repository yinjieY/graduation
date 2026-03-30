-- =========================================================
-- QSGuard Block DB Init Script
-- File: qs-block-service/src/main/resources/sql/init_block_schema.sql
-- Target: MySQL 5.7+ / 8.0+
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `yx_blockchain_proof`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `yx_blockchain_proof`;

DROP TABLE IF EXISTS `blockchain_proof`;
CREATE TABLE `blockchain_proof` (
  `proof_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `business_key` VARCHAR(64) NOT NULL COMMENT 'qsId/eventId',
  `proof_type` VARCHAR(32) NOT NULL COMMENT 'QR_HASH/QR_FREEZE/ALERT_EVENT',
  `hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash',
  `payload` LONGTEXT NOT NULL COMMENT 'Raw business payload',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`proof_id`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_business_type` (`business_key`, `proof_type`),
  KEY `idx_hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Blockchain proof records';

-- relaxed constraint: allow multiple records for same (business_key, proof_type)
INSERT INTO `blockchain_proof` (`business_key`, `proof_type`, `hash`, `payload`)
VALUES
  ('QS000001', 'QR_HASH', 'hash123', '{"demo":true}');

SET FOREIGN_KEY_CHECKS = 1;

