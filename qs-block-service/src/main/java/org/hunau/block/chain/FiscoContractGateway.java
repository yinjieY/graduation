package org.hunau.block.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FISCO-BCOS区块链网关实现（JSON-RPC方式）
 * 直接连接FISCO节点的8545端口（JSON-RPC接口）
 */
@Component
@ConditionalOnProperty(name = "app.blockchain.gateway-mode", havingValue = "fisco")
public class FiscoContractGateway implements BlockchainGateway {

    private static final Logger log = LoggerFactory.getLogger(FiscoContractGateway.class);

    private final RestTemplate restTemplate;

    @Value("${app.blockchain.fisco.write-url}")
    private String writeUrl;

    @Value("${app.blockchain.fisco.verify-url}")
    private String verifyUrl;

    @Value("${app.blockchain.contract-address}")
    private String contractAddress;

    public FiscoContractGateway(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ChainWriteResult writeProof(String businessKey, String proofType, String hash, String payload) {
        try {
            log.info("FISCO JSON-RPC写入存证: businessKey={}, proofType={}", businessKey, proofType);

            // 构建JSON-RPC请求
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", UUID.randomUUID().toString());
            request.put("method", "eth_sendTransaction");

            // 构建交易参数
            Map<String, Object> txParams = new HashMap<>();
            txParams.put("to", contractAddress);
            txParams.put("data", encodeFunctionCall("save", new Object[]{businessKey, proofType, hash, payload}));

            request.put("params", new Object[]{txParams});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    writeUrl,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                if (result != null && !result.containsKey("error")) {
                    String txHash = (String) result.get("result");
                    log.info("FISCO存证成功: businessKey={}, txHash={}", businessKey, txHash);
                    return new ChainWriteResult(txHash, null, contractAddress);
                } else if (result != null && result.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) result.get("error");
                    String errorMsg = (String) error.get("message");
                    log.error("FISCO存证失败: {}", errorMsg);
                    throw new RuntimeException("存证失败: " + errorMsg);
                }
            }
            throw new RuntimeException("HTTP请求失败: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("FISCO存证失败: businessKey={}, error={}", businessKey, e.getMessage(), e);
            throw new RuntimeException("存证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyProof(String businessKey, String hash, String txHash) {
        try {
            log.info("FISCO JSON-RPC验证存证: businessKey={}, hash={}", businessKey, hash);

            // 构建JSON-RPC请求调用verify方法
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", UUID.randomUUID().toString());
            request.put("method", "eth_call");

            Map<String, Object> callParams = new HashMap<>();
            callParams.put("to", contractAddress);
            callParams.put("data", encodeFunctionCall("verify", new Object[]{businessKey, hash}));

            request.put("params", new Object[]{callParams, "latest"});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    verifyUrl,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                if (result != null && !result.containsKey("error")) {
                    String output = (String) result.get("result");
                    // 解析返回值（Solidity bool返回0x00或0x01）
                    boolean verified = !"0x00".equals(output) && !"0x".equals(output);
                    log.info("FISCO验证结果: businessKey={}, verified={}", businessKey, verified);
                    return verified;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("FISCO验证失败: businessKey={}, error={}", businessKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 编码合约函数调用
     * 简化实现：实际需要使用ABI编码
     */
    private String encodeFunctionCall(String functionName, Object[] params) {
        // 简化实现：实际生产中需要使用web3j或其他库进行ABI编码
        // 这里返回一个占位符，实际使用时需要实现真实的ABI编码
        log.warn("使用简化的函数编码，实际生产环境需要ABI编码");
        return "0x" + functionName.hashCode();
    }
}