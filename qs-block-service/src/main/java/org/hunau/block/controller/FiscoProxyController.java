package org.hunau.block.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * FISCO区块链代理控制器
 * 将HTTP请求转发到WSL中的FISCO-BCOS节点JSON-RPC接口
 */
@RestController
@RequestMapping("/block/proof/fisco")
public class FiscoProxyController {

    private static final Logger log = LoggerFactory.getLogger(FiscoProxyController.class);

    private final RestTemplate restTemplate;

    @Value("${app.blockchain.fisco.write-url}")
    private String fiscoNodeUrl;

    public FiscoProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 存证写入代理接口
     */
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> writeProof(@RequestBody Map<String, Object> request) {
        try {
            log.info("接收到存证请求: businessKey={}", request.get("businessKey"));

            // 构建JSON-RPC请求
            Map<String, Object> body = new HashMap<>();
            body.put("jsonrpc", "2.0");
            body.put("id", 1);
            body.put("method", "eth_sendTransaction");

            Map<String, Object> txParams = new HashMap<>();
            txParams.put("to", request.get("contractAddress"));
            txParams.put("data", encodeFunctionCall("save", new Object[]{
                    request.get("businessKey"),
                    request.get("proofType"),
                    request.get("hash"),
                    request.get("payload")
            }));

            body.put("params", new Object[]{txParams});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 转发请求到FISCO节点JSON-RPC接口
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fiscoNodeUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                Map<String, Object> data = new HashMap<>();

                if (result != null && result.containsKey("error")) {
                    data.put("success", false);
                    data.put("message", result.get("error"));
                } else {
                    data.put("success", true);
                    data.put("txHash", result != null ? result.get("result") : null);
                    data.put("message", "存证成功");
                    log.info("存证成功: txHash={}", data.get("txHash"));
                }
                return ResponseEntity.ok(data);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "FISCO节点返回错误: " + response.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        } catch (Exception e) {
            log.error("存证失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "连接FISCO节点失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 存证验证代理接口
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyProof(@RequestBody Map<String, Object> request) {
        try {
            log.info("接收到验证请求: businessKey={}", request.get("businessKey"));

            // 构建JSON-RPC请求
            Map<String, Object> body = new HashMap<>();
            body.put("jsonrpc", "2.0");
            body.put("id", 1);
            body.put("method", "eth_call");

            Map<String, Object> callParams = new HashMap<>();
            callParams.put("to", request.get("contractAddress"));
            callParams.put("data", encodeFunctionCall("verify", new Object[]{
                    request.get("businessKey"),
                    request.get("hash")
            }));

            body.put("params", new Object[]{callParams, "latest"});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fiscoNodeUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> respBody = response.getBody();
                if (respBody.containsKey("error")) {
                    result.put("verified", false);
                    result.put("success", false);
                    result.put("message", respBody.get("error"));
                } else {
                    Object output = respBody.get("result");
                    boolean verified = output != null &&
                            !("0x00".equals(output.toString()) || "0x".equals(output.toString()));
                    result.put("verified", verified);
                    result.put("success", true);
                    log.info("验证结果: businessKey={}, verified={}", request.get("businessKey"), verified);
                }
            } else {
                result.put("verified", false);
                result.put("success", false);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("验证失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("verified", false);
            error.put("success", false);
            error.put("message", "连接FISCO节点失败: " + e.getMessage());
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
            // 使用getBlockNumber方法测试连接
            Map<String, Object> body = new HashMap<>();
            body.put("jsonrpc", "2.0");
            body.put("id", 1);
            body.put("method", "eth_blockNumber");
            body.put("params", new Object[]{});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    fiscoNodeUrl,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                result.put("fiscoNode", "UP");
                result.put("status", "UP");
                if (response.getBody() != null && response.getBody().containsKey("result")) {
                    result.put("blockNumber", response.getBody().get("result"));
                }
            } else {
                result.put("fiscoNode", "DOWN");
                result.put("status", "DEGRADED");
                result.put("error", "HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            result.put("fiscoNode", "DOWN");
            result.put("status", "DEGRADED");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 编码合约函数调用
     * 简化实现：实际生产中需要使用ABI编码
     */
    private String encodeFunctionCall(String functionName, Object[] params) {
        log.warn("使用简化的函数编码，实际生产环境需要ABI编码");
        return "0x" + Integer.toHexString(functionName.hashCode());
    }
}