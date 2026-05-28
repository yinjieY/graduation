package org.hunau.block.chain;

import org.hunau.common.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.blockchain.gateway-mode", havingValue = "mock", matchIfMissing = true)
public class MockFiscoBcosGateway implements BlockchainGateway {

    @Value("${app.blockchain.contract-address:0xMOCK_CONTRACT}")
    private String contractAddress;

    @Override
    public ChainWriteResult writeProof(String businessKey, String proofType, String hash, String payload) {
        String txHash = HashUtil.sha256Hex(businessKey + "|" + proofType + "|" + hash + "|" + System.nanoTime());
        long blockNumber = Math.abs(txHash.hashCode()) + 1L;
        return new ChainWriteResult(txHash, blockNumber, contractAddress);
    }

    @Override
    public boolean verifyProof(String businessKey, String proofType, String hash, String txHash) {
        return txHash != null && !txHash.isBlank() && businessKey != null && hash != null;
    }
}

