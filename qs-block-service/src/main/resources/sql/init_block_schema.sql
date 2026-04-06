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

DROP TABLE IF EXISTS `proof_dead_letter`;
DROP TABLE IF EXISTS `proof_outbox`;
DROP TABLE IF EXISTS `blockchain_proof`;

CREATE TABLE `blockchain_proof` (
  `proof_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `business_key` VARCHAR(64) NOT NULL COMMENT 'qsId/eventId',
  `proof_type` VARCHAR(32) NOT NULL COMMENT 'QR_HASH/QR_FREEZE/ALERT_EVENT',
  `hash` VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash',
  `payload` LONGTEXT NOT NULL COMMENT 'Raw business payload',
  `chain_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  `tx_hash` VARCHAR(128) NULL COMMENT 'On-chain transaction hash',
  `block_number` BIGINT NULL COMMENT 'On-chain block number',
  `contract_address` VARCHAR(128) NULL COMMENT 'Contract address',
  `chain_error` VARCHAR(512) NULL COMMENT 'Last chain write error',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT 'Retry count',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT 'Idempotency key',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`proof_id`),
  UNIQUE KEY `uk_idempotency` (`idempotency_key`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_business_type` (`business_key`, `proof_type`),
  KEY `idx_hash` (`hash`),
  KEY `idx_chain_status` (`chain_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Blockchain proof records';

CREATE TABLE `proof_outbox` (
  `outbox_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `proof_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RETRY/SUCCESS/DEAD',
  `retry_count` INT NOT NULL DEFAULT 0,
  `next_retry_at` DATETIME NOT NULL,
  `last_error` VARCHAR(512) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`outbox_id`),
  KEY `idx_outbox_status_retry` (`status`, `next_retry_at`),
  KEY `idx_outbox_proof` (`proof_id`),
  CONSTRAINT `fk_outbox_proof` FOREIGN KEY (`proof_id`) REFERENCES `blockchain_proof` (`proof_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Async proof write outbox';

CREATE TABLE `proof_dead_letter` (
  `dead_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `outbox_id` BIGINT UNSIGNED NOT NULL,
  `reason` VARCHAR(512) NOT NULL,
  `payload_snapshot` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dead_id`),
  KEY `idx_dead_outbox` (`outbox_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Proof dead-letter records';

INSERT INTO `blockchain_proof`
(`business_key`, `proof_type`, `hash`, `payload`, `chain_status`, `tx_hash`, `contract_address`, `idempotency_key`)
VALUES
('QS000001', 'QR_HASH', 'hash123', '{"demo":true}', 'SUCCESS', 'txhash_demo_1', '0xMOCK_PROOF_REGISTRY', 'idem_demo_1');

SET FOREIGN_KEY_CHECKS = 1;

