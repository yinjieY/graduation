package org.hunau.block.chain;

public interface BlockchainGateway {

    ChainWriteResult writeProof(String businessKey, String proofType, String hash, String payload);

    boolean verifyProof(String businessKey, String proofType, String hash, String txHash);
}

