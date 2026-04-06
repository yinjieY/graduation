package org.hunau.block.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProofRecord {
    private Long proofId;
    private String businessKey;
    private String proofType;
    private String hash;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String chainStatus;
    private String txHash;
    private Long blockNumber;
    private String contractAddress;
    private String chainError;
    private Integer retryCount;
    private String idempotencyKey;
}
