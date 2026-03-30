package org.hunau.trace.client;

import org.hunau.common.R;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class TraceExternalClient {

    private final RestTemplate restTemplate;

    public TraceExternalClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public boolean isCompanyApproved(String companyId) {
        String url = "http://localhost:8084/auth/company/status/" + companyId;
        try {
            R<Map<String, Object>> resp = restTemplate.getForObject(url, R.class);
            if (resp == null || resp.getData() == null) {
                return false;
            }
            Object approved = ((Map<String, Object>) resp.getData()).get("approved");
            return Boolean.TRUE.equals(approved);
        } catch (Exception ex) {
            return false;
        }
    }

    public void saveQrProof(String qsId, String batchId, String companyId, String signature) {
        String url = "http://localhost:8085/block/proof/qr";
        Map<String, Object> body = new HashMap<>();
        body.put("qsId", qsId);
        body.put("batchId", batchId);
        body.put("companyId", companyId);
        body.put("signature", signature);
        restTemplate.postForObject(url, body, R.class);
    }

    public void saveFreezeProof(String qsId, String status, String reason) {
        String url = "http://localhost:8085/block/proof/freeze";
        Map<String, Object> body = new HashMap<>();
        body.put("qsId", qsId);
        body.put("status", status);
        body.put("reason", reason);
        restTemplate.postForObject(url, body, R.class);
    }
}

