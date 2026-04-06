package org.hunau.auth.service;

import org.hunau.auth.client.TraceFeignClient;
import org.hunau.common.R;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyAuthService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final JdbcTemplate jdbcTemplate;
    private final TraceFeignClient traceFeignClient;

    public CompanyAuthService(JdbcTemplate jdbcTemplate, TraceFeignClient traceFeignClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.traceFeignClient = traceFeignClient;
    }

    public boolean isApproved(String companyId) {
        Integer status = getReviewStatus(normalize(companyId));
        return status != null && status == STATUS_APPROVED;
    }

    public Map<String, Object> queryStatus(String companyId) {
        String normalizedCompanyId = normalize(companyId);
        if (normalizedCompanyId.isEmpty()) {
            return null;
        }

        String sql = """
                SELECT company_id, company_name, review_status,
                       apply_by, apply_time,
                       review_by, review_time, remark
                FROM company_auth
                WHERE company_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            int reviewStatus = rs.getInt("review_status");
            data.put("companyId", rs.getString("company_id"));
            data.put("companyName", rs.getString("company_name"));
            data.put("reviewStatus", reviewStatus);
            data.put("statusText", toStatusText(reviewStatus));
            data.put("approved", reviewStatus == STATUS_APPROVED);
            data.put("applyBy", rs.getString("apply_by"));
            data.put("applyTime", rs.getTimestamp("apply_time"));
            data.put("reviewBy", rs.getString("review_by"));
            data.put("reviewTime", rs.getTimestamp("review_time"));
            data.put("remark", rs.getString("remark"));
            return data;
        }, normalizedCompanyId);
    }

    public Map<String, Object> submit(String companyId, String companyName, String remark) {
        String normalizedCompanyId = normalize(companyId);
        String normalizedCompanyName = normalize(companyName);
        if (normalizedCompanyId.isEmpty() || normalizedCompanyName.isEmpty()) {
            throw new IllegalArgumentException("companyId 和 companyName 不能为空");
        }
        String normalizedApplicant = normalize(resolveApplicant());
        if (normalizedApplicant.isEmpty()) {
            normalizedApplicant = "system";
        }

        String sql = """
                INSERT INTO company_auth(company_id, company_name, review_status, apply_by, apply_time, review_by, review_time, remark)
                VALUES (?, ?, 0, ?, NOW(), NULL, NULL, ?)
                ON DUPLICATE KEY UPDATE
                    company_name = VALUES(company_name),
                    review_status = VALUES(review_status),
                    apply_by = VALUES(apply_by),
                    apply_time = VALUES(apply_time),
                    review_by = NULL,
                    review_time = NULL,
                    remark = VALUES(remark)
                """;
        jdbcTemplate.update(sql, normalizedCompanyId, normalizedCompanyName, normalizedApplicant, normalize(remark));
        return queryStatus(normalizedCompanyId);
    }

    private String resolveApplicant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }
        return authentication.getName();
    }

    public Map<String, Object> review(String companyId, String companyName, boolean approved, String reviewer, String remark) {
        String normalizedCompanyId = normalize(companyId);
        String normalizedCompanyName = normalize(companyName);
        if (normalizedCompanyId.isEmpty() || normalizedCompanyName.isEmpty()) {
            throw new IllegalArgumentException("companyId 和 companyName 不能为空");
        }

        int reviewStatus = approved ? STATUS_APPROVED : STATUS_REJECTED;
        String sql = """
                INSERT INTO company_auth(company_id, company_name, review_status, review_by, review_time, remark)
                VALUES (?, ?, ?, ?, NOW(), ?)
                ON DUPLICATE KEY UPDATE
                    company_name = VALUES(company_name),
                    review_status = VALUES(review_status),
                    review_by = VALUES(review_by),
                    review_time = VALUES(review_time),
                    remark = VALUES(remark)
                """;
        String normalizedReviewer = normalize(reviewer);
        if (normalizedReviewer.isEmpty()) {
            normalizedReviewer = "system";
        }
        String finalRemark = normalize(remark);
        if (finalRemark.isEmpty()) {
            finalRemark = approved ? "review approved" : "review rejected";
        }

        jdbcTemplate.update(sql, normalizedCompanyId, normalizedCompanyName, reviewStatus, normalizedReviewer, finalRemark);
        Map<String, Object> data = queryStatus(normalizedCompanyId);
        if (data == null) {
            data = new LinkedHashMap<>();
            data.put("companyId", normalizedCompanyId);
            data.put("companyName", normalizedCompanyName);
            data.put("reviewStatus", reviewStatus);
            data.put("statusText", toStatusText(reviewStatus));
            data.put("approved", approved);
        }

        if (approved) {
            initTraceCompany(normalizedCompanyId, normalizedCompanyName, data);
        }
        return data;
    }

    private void initTraceCompany(String companyId, String companyName, Map<String, Object> reviewData) {
        Map<String, Object> body = new HashMap<>();
        body.put("companyId", companyId);
        body.put("name", companyName);

        try {
            R<Map<String, Object>> resp = traceFeignClient.initCompany(body);
            boolean initSuccess = resp != null && resp.getCode() == 200;
            reviewData.put("traceInitSuccess", initSuccess);
            if (!initSuccess) {
                reviewData.put("traceInitMsg", resp == null ? "trace init response is null" : resp.getMsg());
            }
        } catch (Exception ex) {
            reviewData.put("traceInitSuccess", false);
            reviewData.put("traceInitMsg", "trace init exception: " + ex.getMessage());
        }
    }

    public List<Map<String, Object>> pendingList() {
        String sql = """
                SELECT company_id, company_name, review_status,
                       apply_by, apply_time,
                       review_by, review_time, remark
                FROM company_auth
                WHERE review_status = 0
                ORDER BY updated_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            int reviewStatus = rs.getInt("review_status");
            data.put("companyId", rs.getString("company_id"));
            data.put("companyName", rs.getString("company_name"));
            data.put("reviewStatus", reviewStatus);
            data.put("statusText", toStatusText(reviewStatus));
            data.put("applyBy", rs.getString("apply_by"));
            data.put("applyTime", rs.getTimestamp("apply_time"));
            data.put("reviewBy", rs.getString("review_by"));
            data.put("reviewTime", rs.getTimestamp("review_time"));
            data.put("remark", rs.getString("remark"));
            return data;
        });
    }

    public String findCompanyIdByUsername(String username) {
        String normalizedUsername = normalize(username);
        if (normalizedUsername.isEmpty()) {
            return "";
        }
        String sql = "SELECT company_id FROM auth_user WHERE username = ? AND deleted = 0 LIMIT 1";
        String companyId = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, normalizedUsername);
        return normalize(companyId);
    }

    private Integer getReviewStatus(String companyId) {
        if (companyId.isEmpty()) {
            return null;
        }
        String sql = "SELECT review_status FROM company_auth WHERE company_id = ? LIMIT 1";
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getInt(1) : null, companyId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toStatusText(int status) {
        if (status == STATUS_APPROVED) {
            return "APPROVED";
        }
        if (status == STATUS_REJECTED) {
            return "REJECTED";
        }
        return "PENDING";
    }
}
