package org.hunau.block.chain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.blockchain.gateway-mode", havingValue = "fisco")
public class FiscoContractGateway implements BlockchainGateway {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.blockchain.group-id:group0}")
    private String groupId;

    @Value("${app.blockchain.chain-id:chain0}")
    private String chainId;

    @Value("${app.blockchain.contract-address:}")
    private String contractAddress;

    @Value("${app.blockchain.fisco.write-url:}")
    private String writeUrl;

    @Value("${app.blockchain.fisco.verify-url:}")
    private String verifyUrl;

    @Value("${app.blockchain.fisco.auth-token:}")
    private String authToken;

    @Override
    public ChainWriteResult writeProof(String businessKey, String proofType, String hash, String payload) {
        if (isBlank(writeUrl)) {
            throw new IllegalStateException("FISCO gateway write-url is blank");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("businessKey", businessKey);
        body.put("proofType", proofType);
        body.put("hash", hash);
        body.put("payload", payload);
        body.put("groupId", groupId);
        body.put("chainId", chainId);
        body.put("contractAddress", contractAddress);

        Map<String, Object> resp = postForMap(writeUrl, body);
        String txHash = firstString(resp, "txHash", "transactionHash", "tx_hash", "data.txHash", "data.transactionHash");
        Long blockNumber = firstLong(resp, "blockNumber", "block_number", "data.blockNumber", "data.block_number");
        String resolvedContract = firstString(resp, "contractAddress", "contract_address", "data.contractAddress", "data.contract_address");

        if (isBlank(txHash)) {
            throw new IllegalStateException("FISCO write response missing txHash");
        }
        if (blockNumber == null) {
            blockNumber = 0L;
        }
        if (isBlank(resolvedContract)) {
            resolvedContract = contractAddress;
        }
        return new ChainWriteResult(txHash, blockNumber, resolvedContract);
    }

    @Override
    public boolean verifyProof(String businessKey, String hash, String txHash) {
        if (isBlank(verifyUrl)) {
            throw new IllegalStateException("FISCO gateway verify-url is blank");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("businessKey", businessKey);
        body.put("hash", hash);
        body.put("txHash", txHash);
        body.put("groupId", groupId);
        body.put("chainId", chainId);
        body.put("contractAddress", contractAddress);

        Map<String, Object> resp = postForMap(verifyUrl, body);
        Boolean verified = firstBoolean(resp, "verified", "success", "exists", "data.verified", "data.success", "data.exists");
        return Boolean.TRUE.equals(verified);
    }

    private Map<String, Object> postForMap(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!isBlank(authToken)) {
            headers.set(HttpHeaders.AUTHORIZATION, authToken.startsWith("Bearer ") ? authToken : ("Bearer " + authToken));
        }

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        if (response.getBody() == null) {
            return Map.of();
        }
        return response.getBody();
    }

    private String firstString(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = readPath(source, key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private Long firstLong(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = readPath(source, key);
            if (value == null) {
                continue;
            }
            if (value instanceof Number n) {
                return n.longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (Exception ignored) {
                // try next key
            }
        }
        return null;
    }

    private Boolean firstBoolean(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = readPath(source, key);
            if (value == null) {
                continue;
            }
            if (value instanceof Boolean b) {
                return b;
            }
            String text = String.valueOf(value).trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text) || "ok".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        return null;
    }

    private Object readPath(Map<String, Object> source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

