package org.hunau.block.service;

import org.hunau.block.chain.BlockchainGateway;
import org.hunau.block.chain.ChainWriteResult;
import org.hunau.block.model.ProofRecord;
import org.hunau.common.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProofService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RETRY = "RETRY";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DEAD = "DEAD";

    private final JdbcTemplate jdbcTemplate;
    private final BlockchainGateway blockchainGateway;

    @Value("${app.blockchain.outbox.max-retries:3}")
    private int maxRetries;

    @Value("${app.blockchain.outbox.retry-backoff-seconds:10}")
    private int retryBackoffSeconds;

    @Value("${app.blockchain.outbox.dispatch-batch-size:50}")
    private int dispatchBatchSize;

    public ProofService(JdbcTemplate jdbcTemplate, BlockchainGateway blockchainGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.blockchainGateway = blockchainGateway;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProofRecord save(String businessKey, String proofType, String payload) {
        String hash = HashUtil.sha256Hex(payload);
        String idempotencyKey = HashUtil.sha256Hex(businessKey + "|" + proofType + "|" + hash);

        ProofRecord existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO blockchain_proof(business_key, proof_type, hash, payload, created_at, updated_at,
                                             chain_status, retry_count, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, businessKey);
            ps.setString(2, proofType);
            ps.setString(3, hash);
            ps.setString(4, payload);
            ps.setObject(5, now);
            ps.setObject(6, now);
            ps.setString(7, STATUS_PENDING);
            ps.setInt(8, 0);
            ps.setString(9, idempotencyKey);
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("proof_id生成失败");
        }
        long proofId = keyHolder.getKey().longValue();

        jdbcTemplate.update(
                "INSERT INTO proof_outbox(proof_id, status, retry_count, next_retry_at, created_at, updated_at) VALUES (?, ?, 0, ?, ?, ?)",
                proofId,
                STATUS_PENDING,
                now,
                now,
                now
        );

        // Try once immediately, then rely on scheduled dispatcher for retries.
        processOutboxByProofId(proofId);
        return getById(proofId);
    }

    public boolean verify(String businessKey, String hash) {
        ProofRecord record = findLatestByBusinessKeyAndHash(businessKey, hash);
        if (record == null) {
            return false;
        }
        if (!STATUS_SUCCESS.equals(record.getChainStatus())) {
            return false;
        }
        return blockchainGateway.verifyProof(record.getBusinessKey(), record.getHash(), record.getTxHash());
    }

    public Map<String, Object> verifyDetail(String businessKey, String hash) {
        ProofRecord record = findLatestByBusinessKeyAndHash(businessKey, hash);
        Map<String, Object> data = new HashMap<>();
        data.put("businessKey", businessKey);
        data.put("hash", hash);
        if (record == null) {
            data.put("localExists", false);
            data.put("chainVerified", false);
            data.put("verified", false);
            return data;
        }

        boolean chainVerified = STATUS_SUCCESS.equals(record.getChainStatus())
                && blockchainGateway.verifyProof(record.getBusinessKey(), record.getHash(), record.getTxHash());
        data.put("localExists", true);
        data.put("chainStatus", record.getChainStatus());
        data.put("txHash", record.getTxHash());
        data.put("contractAddress", record.getContractAddress());
        data.put("chainVerified", chainVerified);
        data.put("verified", chainVerified);
        return data;
    }

    public List<ProofRecord> list(String businessKey) {
        String sql = """
                SELECT proof_id, business_key, proof_type, hash, payload, created_at, updated_at,
                       chain_status, tx_hash, block_number, contract_address, chain_error, retry_count, idempotency_key
                FROM blockchain_proof
                WHERE business_key = ?
                ORDER BY proof_id DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProofRecord record = new ProofRecord();
            record.setProofId(rs.getLong("proof_id"));
            record.setBusinessKey(rs.getString("business_key"));
            record.setProofType(rs.getString("proof_type"));
            record.setHash(rs.getString("hash"));
            record.setPayload(rs.getString("payload"));
            record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            record.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
            record.setChainStatus(rs.getString("chain_status"));
            record.setTxHash(rs.getString("tx_hash"));
            long blockNumber = rs.getLong("block_number");
            record.setBlockNumber(rs.wasNull() ? null : blockNumber);
            record.setContractAddress(rs.getString("contract_address"));
            record.setChainError(rs.getString("chain_error"));
            record.setRetryCount(rs.getInt("retry_count"));
            record.setIdempotencyKey(rs.getString("idempotency_key"));
            return record;
        }, businessKey);
    }

    public int dispatchPendingOutbox() {
        List<Long> outboxIds = jdbcTemplate.query(
                "SELECT outbox_id FROM proof_outbox WHERE status IN (?, ?) AND next_retry_at <= NOW() ORDER BY next_retry_at ASC LIMIT ?",
                (rs, rowNum) -> rs.getLong("outbox_id"),
                STATUS_PENDING,
                STATUS_RETRY,
                dispatchBatchSize
        );

        int success = 0;
        for (Long outboxId : outboxIds) {
            if (processOutbox(outboxId)) {
                success++;
            }
        }
        return success;
    }

    private void processOutboxByProofId(Long proofId) {
        Long outboxId = jdbcTemplate.query(
                "SELECT outbox_id FROM proof_outbox WHERE proof_id = ? ORDER BY outbox_id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong("outbox_id") : null,
                proofId
        );
        if (outboxId != null) {
            processOutbox(outboxId);
        }
    }

    protected boolean processOutbox(Long outboxId) {
        Map<String, Object> row = jdbcTemplate.query(
                "SELECT outbox_id, proof_id, status, retry_count FROM proof_outbox WHERE outbox_id = ? FOR UPDATE",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("outbox_id", rs.getLong("outbox_id"));
                    map.put("proof_id", rs.getLong("proof_id"));
                    map.put("status", rs.getString("status"));
                    map.put("retry_count", rs.getInt("retry_count"));
                    return map;
                },
                outboxId
        );
        if (row == null) {
            return false;
        }

        long proofId = ((Number) row.get("proof_id")).longValue();
        int retryCount = ((Number) row.get("retry_count")).intValue();
        ProofRecord proof = getById(proofId);
        if (proof == null) {
            markDead(outboxId, retryCount + 1, "proof_not_found");
            return false;
        }

        try {
            ChainWriteResult result = blockchainGateway.writeProof(
                    proof.getBusinessKey(),
                    proof.getProofType(),
                    proof.getHash(),
                    proof.getPayload()
            );

            jdbcTemplate.update(
                    "UPDATE blockchain_proof SET chain_status=?, tx_hash=?, block_number=?, contract_address=?, chain_error=NULL, updated_at=NOW(), retry_count=? WHERE proof_id=?",
                    STATUS_SUCCESS,
                    result.txHash(),
                    result.blockNumber(),
                    result.contractAddress(),
                    retryCount,
                    proofId
            );
            jdbcTemplate.update(
                    "UPDATE proof_outbox SET status=?, updated_at=NOW(), last_error=NULL WHERE outbox_id=?",
                    STATUS_SUCCESS,
                    outboxId
            );
            return true;
        } catch (Exception ex) {
            int nextRetry = retryCount + 1;
            String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();

            jdbcTemplate.update(
                    "UPDATE blockchain_proof SET chain_status=?, chain_error=?, updated_at=NOW(), retry_count=? WHERE proof_id=?",
                    STATUS_FAILED,
                    error,
                    nextRetry,
                    proofId
            );

            if (nextRetry > Math.max(0, maxRetries)) {
                markDead(outboxId, nextRetry, error);
                return false;
            }

            int delaySeconds = Math.max(1, retryBackoffSeconds) * nextRetry;
            jdbcTemplate.update(
                    "UPDATE proof_outbox SET status=?, retry_count=?, last_error=?, next_retry_at=DATE_ADD(NOW(), INTERVAL ? SECOND), updated_at=NOW() WHERE outbox_id=?",
                    STATUS_RETRY,
                    nextRetry,
                    error,
                    delaySeconds,
                    outboxId
            );
            return false;
        }
    }

    private void markDead(Long outboxId, int retryCount, String error) {
        jdbcTemplate.update(
                "UPDATE proof_outbox SET status=?, retry_count=?, last_error=?, updated_at=NOW() WHERE outbox_id=?",
                STATUS_DEAD,
                retryCount,
                error,
                outboxId
        );
        jdbcTemplate.update(
                "INSERT INTO proof_dead_letter(outbox_id, reason, payload_snapshot, created_at) " +
                        "SELECT outbox_id, ?, CONCAT('proof_id=', proof_id), NOW() FROM proof_outbox WHERE outbox_id=?",
                error,
                outboxId
        );
    }

    private ProofRecord findByIdempotencyKey(String idempotencyKey) {
        return jdbcTemplate.query(
                "SELECT proof_id FROM blockchain_proof WHERE idempotency_key = ? LIMIT 1",
                rs -> rs.next() ? getById(rs.getLong("proof_id")) : null,
                idempotencyKey
        );
    }

    private ProofRecord findLatestByBusinessKeyAndHash(String businessKey, String hash) {
        return jdbcTemplate.query(
                "SELECT proof_id FROM blockchain_proof WHERE business_key = ? AND hash = ? ORDER BY proof_id DESC LIMIT 1",
                rs -> rs.next() ? getById(rs.getLong("proof_id")) : null,
                businessKey,
                hash
        );
    }

    private ProofRecord getById(Long proofId) {
        return jdbcTemplate.query(
                "SELECT proof_id, business_key, proof_type, hash, payload, created_at, updated_at, chain_status, tx_hash, block_number, contract_address, chain_error, retry_count, idempotency_key " +
                        "FROM blockchain_proof WHERE proof_id = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    ProofRecord record = new ProofRecord();
                    record.setProofId(rs.getLong("proof_id"));
                    record.setBusinessKey(rs.getString("business_key"));
                    record.setProofType(rs.getString("proof_type"));
                    record.setHash(rs.getString("hash"));
                    record.setPayload(rs.getString("payload"));
                    record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    record.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
                    record.setChainStatus(rs.getString("chain_status"));
                    record.setTxHash(rs.getString("tx_hash"));
                    long blockNumber = rs.getLong("block_number");
                    record.setBlockNumber(rs.wasNull() ? null : blockNumber);
                    record.setContractAddress(rs.getString("contract_address"));
                    record.setChainError(rs.getString("chain_error"));
                    record.setRetryCount(rs.getInt("retry_count"));
                    record.setIdempotencyKey(rs.getString("idempotency_key"));
                    return record;
                },
                proofId
        );
    }
}
