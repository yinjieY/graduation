package org.hunau.trace.client;

import org.hunau.common.R;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TraceExternalClient {

    private final AuthFeignClient authFeignClient;
    private final BlockFeignClient blockFeignClient;

    public TraceExternalClient(AuthFeignClient authFeignClient, BlockFeignClient blockFeignClient) {
        this.authFeignClient = authFeignClient;
        this.blockFeignClient = blockFeignClient;
    }

    public boolean isCompanyApproved(String companyId) {
        try {
            R<Map<String, Object>> resp = authFeignClient.queryCompanyStatus(companyId);
            if (resp == null || resp.getCode() != 200 || resp.getData() == null) {
                return false;
            }
            Object approved = resp.getData().get("approved");
            if (approved instanceof String approvedText) {
                return Boolean.parseBoolean(approvedText);
            }
            return Boolean.TRUE.equals(approved);
        } catch (Exception ex) {
            return false;
        }
    }

    public void saveQrProof(String qsId,
                            String batchId,
                            String companyId,
                            String signature,
                            String signaturePayload,
                            String qsUrl,
                            String issueTime) {
        Map<String, Object> body = new HashMap<>();
        body.put("qsId", qsId);
        body.put("batchId", batchId);
        body.put("companyId", companyId);
        body.put("signature", signature);
        body.put("signaturePayload", signaturePayload);
        body.put("qsUrl", qsUrl);
        body.put("issueTime", issueTime);
        body.put("proofTime", java.time.LocalDateTime.now().toString());
        body.put("proofVersion", "v1");
        try {
            blockFeignClient.saveQrProof(body);
        } catch (Exception ignored) {
            // keep trace issuance path non-blocking when proof service is unstable
        }
    }

    public void saveFreezeProof(String qsId, String status, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("qsId", qsId);
        body.put("status", status);
        body.put("reason", reason);
        try {
            blockFeignClient.saveFreezeProof(body);
        } catch (Exception ignored) {
            // keep trace status path non-blocking when proof service is unstable
        }
    }

    public void updateCompanyAuthInfo(String companyId, String companyName) {
        Map<String, String> body = new HashMap<>();
        body.put("companyId", companyId);
        body.put("companyName", companyName);
        try {
            authFeignClient.updateCompanyInfo(body);
        } catch (Exception ignored) {
            // keep trace update path non-blocking when auth service is unstable
        }
    }
}

