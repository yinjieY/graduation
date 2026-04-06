-- Upgrade blockchain proof schema to support on-chain metadata and outbox retry governance
USE `yx_blockchain_proof`;

ALTER TABLE `blockchain_proof`
    ADD COLUMN IF NOT EXISTS `chain_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED' AFTER `payload`,
    ADD COLUMN IF NOT EXISTS `tx_hash` VARCHAR(128) NULL COMMENT 'On-chain transaction hash' AFTER `chain_status`,
    ADD COLUMN IF NOT EXISTS `block_number` BIGINT NULL COMMENT 'On-chain block number' AFTER `tx_hash`,
    ADD COLUMN IF NOT EXISTS `contract_address` VARCHAR(128) NULL COMMENT 'Contract address' AFTER `block_number`,
    ADD COLUMN IF NOT EXISTS `chain_error` VARCHAR(512) NULL COMMENT 'Last chain write error' AFTER `contract_address`,
    ADD COLUMN IF NOT EXISTS `retry_count` INT NOT NULL DEFAULT 0 COMMENT 'Retry count' AFTER `chain_error`,
    ADD COLUMN IF NOT EXISTS `idempotency_key` VARCHAR(64) NULL COMMENT 'Idempotency key' AFTER `retry_count`,
    ADD COLUMN IF NOT EXISTS `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

CREATE UNIQUE INDEX IF NOT EXISTS `uk_idempotency` ON `blockchain_proof`(`idempotency_key`);
CREATE INDEX IF NOT EXISTS `idx_chain_status` ON `blockchain_proof`(`chain_status`);

UPDATE `blockchain_proof`
SET `idempotency_key` = SHA2(CONCAT(`business_key`, '|', `proof_type`, '|', `hash`), 256)
WHERE `idempotency_key` IS NULL;

CREATE TABLE IF NOT EXISTS `proof_outbox` (
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

CREATE TABLE IF NOT EXISTS `proof_dead_letter` (
    `dead_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `outbox_id` BIGINT UNSIGNED NOT NULL,
    `reason` VARCHAR(512) NOT NULL,
    `payload_snapshot` LONGTEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`dead_id`),
    KEY `idx_dead_outbox` (`outbox_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Proof dead-letter records';

