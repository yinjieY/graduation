package org.hunau.block.controller;

import org.hunau.block.chain.BlockchainGateway;
import org.hunau.block.chain.ChainWriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块链接入代理控制器
 * 当前统一走BlockchainGateway（可切换为WeBASE-SDK）。
 */
@RestController
@RequestMapping("/block/proof/fisco")
public class FiscoProxyController {

    private static final Logger log = LoggerFactory.getLogger(FiscoProxyController.class);

    private final BlockchainGateway blockchainGateway;

    @Value("${app.blockchain.gateway-mode:unknown}")
    private String gatewayMode;

    public FiscoProxyController(BlockchainGateway blockchainGateway) {
        this.blockchainGateway = blockchainGateway;
    }

    /**
     * 存证写入代理接口
     */
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> writeProof(@RequestBody Map<String, Object> request) {
        try {
            String businessKey = String.valueOf(request.getOrDefault("businessKey", "")).trim();
            String proofType = String.valueOf(request.getOrDefault("proofType", "QR_HASH")).trim();
            String hash = String.valueOf(request.getOrDefault("hash", "")).trim();
            String payload = String.valueOf(request.getOrDefault("payload", "{}")).trim();
            ChainWriteResult writeResult = blockchainGateway.writeProof(businessKey, proofType, hash, payload);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("txHash", writeResult.txHash());
            data.put("blockNumber", writeResult.blockNumber());
            data.put("contractAddress", writeResult.contractAddress());
            data.put("message", "存证成功");
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("存证失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "链上调用失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 存证验证代理接口
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyProof(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = new HashMap<>();
            String businessKey = String.valueOf(request.getOrDefault("businessKey", "")).trim();
            String hash = String.valueOf(request.getOrDefault("hash", "")).trim();
            String txHash = String.valueOf(request.getOrDefault("txHash", "")).trim();
            boolean verified = blockchainGateway.verifyProof(businessKey, hash, txHash);

            result.put("verified", verified);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("验证失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("verified", false);
            error.put("success", false);
            error.put("message", "链上调用失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("fiscoNode", "UP");
            result.put("status", "UP");
            result.put("gatewayMode", gatewayMode);
            result.put("gatewayClass", blockchainGateway.getClass().getSimpleName());
            result.put("message", "Gateway已装配，可通过/block/proof/qr与/block/proof/verify做链路验证");
        } catch (Exception e) {
            result.put("fiscoNode", "DOWN");
            result.put("status", "DEGRADED");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}