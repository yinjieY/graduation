package org.hunau.block.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WeBASE-SDK网关实现
 * 通过WeBASE-Front接口完成交易发送与只读调用，不再直连节点JSON-RPC。
 */
@Component
@ConditionalOnProperty(name = "app.blockchain.gateway-mode", havingValue = "webase-sdk")
public class FiscoContractGateway implements BlockchainGateway {

    private static final Logger log = LoggerFactory.getLogger(FiscoContractGateway.class);

    private final RestTemplate restTemplate;

    @Value("${app.blockchain.webase.base-url}")
    private String webaseBaseUrl;

    @Value("${app.blockchain.webase.user:}")
    private String webaseUser;

    @Value("${app.blockchain.webase.group-id:${app.blockchain.group-id:group0}}")
    private String webaseGroupId;

    @Value("${app.blockchain.contract-address}")
    private String contractAddress;

    @Value("${app.blockchain.contract-name:ProofContract}")
    private String contractName;

    private static final List<Map<String, Object>> CONTRACT_ABI = Arrays.asList(
        createSaveAbi("save", Arrays.asList(
            createParam("businessKey", "string"),
            createParam("proofType", "string"),
            createParam("hash", "string"),
            createParam("payload", "string")
        )),
        createVerifyAbi("verify", Arrays.asList(
            createParam("businessKey", "string"),
            createParam("hash", "string")
        ))
    );

    private static Map<String, Object> createParam(String name, String type) {
        Map<String, Object> param = new HashMap<>();
        param.put("name", name);
        param.put("type", type);
        return param;
    }

    private static Map<String, Object> createSaveAbi(String name, List<Map<String, Object>> inputs) {
        Map<String, Object> abi = new HashMap<>();
        abi.put("constant", false);
        abi.put("inputs", inputs);
        abi.put("name", name);
        abi.put("outputs", Collections.emptyList());
        abi.put("payable", false);
        abi.put("stateMutability", "nonpayable");
        abi.put("type", "function");
        return abi;
    }

    private static Map<String, Object> createVerifyAbi(String name, List<Map<String, Object>> inputs) {
        Map<String, Object> abi = new HashMap<>();
        abi.put("constant", true);
        abi.put("inputs", inputs);
        abi.put("name", name);
        abi.put("outputs", Collections.singletonList(Collections.singletonMap("type", "bool")));
        abi.put("payable", false);
        abi.put("stateMutability", "view");
        abi.put("type", "function");
        return abi;
    }

    public FiscoContractGateway(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ChainWriteResult writeProof(String businessKey, String proofType, String hash, String payload) {
        try {
            log.info("=== WeBASE-SDK写入存证 === businessKey={}, proofType={}, contract={}", businessKey, proofType, contractAddress);

            Map<String, Object> request = new HashMap<>();
            request.put("groupId", String.valueOf(parseGroupId(webaseGroupId)));
            request.put("contractAddress", contractAddress);
            request.put("contractName", contractName);
            request.put("contractPath", "/");
            request.put("version", "");
            request.put("funcName", "save");
            request.put("funcParam", Arrays.asList(businessKey, proofType, hash, payload));
            request.put("contractAbi", CONTRACT_ABI);
            request.put("useAes", false);
            request.put("useCns", false);
            request.put("cnsName", "");
            putUserIfPresent(request);

            Map<String, Object> response = invokeWeBase(
                    Collections.singletonList("/trans/handle"),
                    request
            );

            String txHash = extractString(response,
                    "transactionHash", "txHash", "transHash", "result", "hash");
            if (txHash == null || txHash.isBlank()) {
                throw new RuntimeException("WeBASE返回成功但未解析到txHash: " + response);
            }

            Long blockNumber = extractLong(response,
                    "blockNumber", "blockNum", "transactionBlockNumber");
            log.info("WeBASE写入成功: txHash={}, blockNumber={}", txHash, blockNumber);
            return new ChainWriteResult(txHash, blockNumber, contractAddress);
        } catch (Exception e) {
            log.error("存证失败！", e);
            throw new RuntimeException("存证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 groupId 字段。
     * group0 -> 1；数字字符串直接使用。
     */
    private int parseGroupId(String gid) {
        try {
            if (gid == null || gid.isBlank()) return 1;
            String normalized = gid.trim().toLowerCase();
            if ("group0".equals(normalized) || "0".equals(normalized)) {
                return 1;
            }
            if (normalized.startsWith("group")) {
                normalized = normalized.substring(5);
            }
            int parsed = Integer.parseInt(normalized);
            return parsed > 0 ? parsed : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public boolean verifyProof(String businessKey, String hash, String txHash) {
        try {
            log.info("WeBASE验证存证: businessKey={}, hash={}", businessKey, hash);

            Map<String, Object> request = new HashMap<>();
            request.put("groupId", String.valueOf(parseGroupId(webaseGroupId)));
            request.put("contractAddress", contractAddress);
            request.put("contractName", contractName);
            request.put("contractPath", "/");
            request.put("version", "");
            request.put("funcName", "verify");
            request.put("funcParam", Arrays.asList(businessKey, hash));
            request.put("contractAbi", CONTRACT_ABI);
            request.put("useAes", false);
            request.put("useCns", false);
            request.put("cnsName", "");
            putUserIfPresent(request);

            Map<String, Object> response = invokeWeBase(
                    Arrays.asList("/trans/constantCall", "/trans/call"),
                    request
            );
            boolean verified = extractBoolean(response);
            log.info("WeBASE验证结果: {}", verified);
            return verified;
        } catch (Exception e) {
            log.error("验证失败", e);
            return false;
        }
    }

    private Map<String, Object> invokeWeBase(List<String> paths, Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RuntimeException lastException = null;
        for (String path : paths) {
            try {
                String url = normalizeBaseUrl(webaseBaseUrl) + path;
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        url,
                        new HttpEntity<>(request, headers),
                        Map.class
                );
                if (!response.getStatusCode().is2xxSuccessful()) {
                    lastException = new RuntimeException("HTTP " + response.getStatusCode() + " @ " + path);
                    continue;
                }
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    lastException = new RuntimeException("WeBASE空响应 @ " + path);
                    continue;
                }
                if (hasExplicitError(body)) {
                    lastException = new RuntimeException("WeBASE返回错误 @ " + path + ": " + body);
                    continue;
                }
                return body;
            } catch (Exception ex) {
                lastException = new RuntimeException("调用失败 @ " + path + ": " + ex.getMessage(), ex);
            }
        }
        throw lastException == null ? new RuntimeException("WeBASE调用失败") : lastException;
    }

    private void putUserIfPresent(Map<String, Object> request) {
        if (webaseUser != null && !webaseUser.isBlank()) {
            request.put("user", webaseUser.trim());
        }
    }

    private boolean hasExplicitError(Map<String, Object> body) {
        if (body.containsKey("error") && body.get("error") != null) {
            return true;
        }
        Object codeObj = body.get("code");
        if (codeObj == null) {
            return false;
        }
        String code = String.valueOf(codeObj).trim();
        return !("0".equals(code) || "200".equals(code) || "SUCCESS".equalsIgnoreCase(code));
    }

    private boolean extractBoolean(Map<String, Object> body) {
        Object value = extractByKeys(body, Arrays.asList("output", "result", "data", "value"));
        return toBoolean(value);
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty() && toBoolean(list.get(0));
        }
        if (value instanceof Map<?, ?> map) {
            return toBoolean(extractByKeys((Map<String, Object>) map, Arrays.asList("output", "result", "value", "data")));
        }

        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return false;
        }
        if ("true".equalsIgnoreCase(str)) {
            return true;
        }
        if ("false".equalsIgnoreCase(str)) {
            return false;
        }
        if (str.startsWith("0x")) {
            return ContractAbiEncoder.decodeBoolResult(str);
        }
        return "1".equals(str);
    }

    private String extractString(Map<String, Object> body, String... keys) {
        Object value = extractByKeys(body, Arrays.asList(keys));
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private Long extractLong(Map<String, Object> body, String... keys) {
        Object value = extractByKeys(body, Arrays.asList(keys));
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return null;
        }
        if (str.startsWith("0x")) {
            return hexToLong(str);
        }
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return null;
        }
    }

    private Object extractByKeys(Map<String, Object> body, List<String> keys) {
        if (body == null) {
            return null;
        }
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return body.get(key);
            }
        }
        for (Object value : body.values()) {
            if (value instanceof Map<?, ?> nested) {
                Object found = extractByKeys((Map<String, Object>) nested, keys);
                if (found != null) {
                    return found;
                }
            }
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> nestedItem) {
                        Object found = extractByKeys((Map<String, Object>) nestedItem, keys);
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.blockchain.webase.base-url 未配置");
        }
        String value = baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 十六进制转Long
     */
    private Long hexToLong(String hex) {
        try {
            if (hex.startsWith("0x")) {
                return new BigInteger(hex.substring(2), 16).longValue();
            }
            return new BigInteger(hex, 16).longValue();
        } catch (Exception e) {
            return null;
        }
    }
}
