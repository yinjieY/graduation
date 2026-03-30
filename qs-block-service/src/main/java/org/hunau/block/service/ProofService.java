package org.hunau.block.service;

import org.hunau.block.model.ProofRecord;
import org.hunau.common.util.HashUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProofService {

    private final JdbcTemplate jdbcTemplate;

    public ProofService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProofRecord save(String businessKey, String proofType, String payload) {
        ProofRecord record = new ProofRecord();
        record.setBusinessKey(businessKey);
        record.setProofType(proofType);
        record.setPayload(payload);
        record.setHash(HashUtil.sha256Hex(payload));
        record.setCreatedAt(LocalDateTime.now());

        String sql = """
                INSERT INTO blockchain_proof(business_key, proof_type, hash, payload, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, record.getBusinessKey());
            ps.setString(2, record.getProofType());
            ps.setString(3, record.getHash());
            ps.setString(4, record.getPayload());
            ps.setObject(5, record.getCreatedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            record.setProofId(keyHolder.getKey().longValue());
        }
        return record;
    }

    public boolean verify(String businessKey, String hash) {
        String sql = "SELECT COUNT(1) FROM blockchain_proof WHERE business_key = ? AND hash = ?";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, businessKey, hash);
        return cnt > 0;
    }

    public List<ProofRecord> list(String businessKey) {
        String sql = """
                SELECT proof_id, business_key, proof_type, hash, payload, created_at
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
            return record;
        }, businessKey);
    }
}

