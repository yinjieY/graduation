package org.hunau.auth.service;

import org.hunau.auth.client.AlertFeignClient;
import org.hunau.auth.client.TraceFeignClient;
import org.hunau.common.model.R;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CompanyAuthService {

    private static final int STATUS_NOT_APPLIED = -1;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final DateTimeFormatter COMPANY_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JdbcTemplate jdbcTemplate;
    private final TraceFeignClient traceFeignClient;
    private final AlertFeignClient alertFeignClient;

    public CompanyAuthService(JdbcTemplate jdbcTemplate, TraceFeignClient traceFeignClient, 
                             AlertFeignClient alertFeignClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.traceFeignClient = traceFeignClient;
        this.alertFeignClient = alertFeignClient;
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
        if (normalizedCompanyId.isEmpty()) {
            throw new IllegalArgumentException("companyId 不能为空，请先绑定企业账号");
        }
        if (normalizedCompanyName.isEmpty()) {
            throw new IllegalArgumentException("companyName 不能为空");
        }

        Integer currentStatus = getReviewStatus(normalizedCompanyId);
        if (currentStatus != null && currentStatus == STATUS_PENDING) {
            throw new IllegalArgumentException("企业认证申请正在审核中，请勿重复提交");
        }
        
        String normalizedApplicant = normalize(resolveApplicant());
        if (normalizedApplicant.isEmpty()) {
            normalizedApplicant = "system";
        }

        String insertSql = """
                INSERT INTO company_auth(company_id, company_name, review_status, apply_by, apply_time, review_by, review_time, remark)
                VALUES (?, ?, 0, ?, NOW(), NULL, NULL, ?)
                ON DUPLICATE KEY UPDATE
                    company_name = VALUES(company_name),
                    review_status = 0,
                    apply_by = VALUES(apply_by),
                    apply_time = VALUES(apply_time),
                    review_by = NULL,
                    review_time = NULL,
                    remark = VALUES(remark)
                """;
        jdbcTemplate.update(insertSql, normalizedCompanyId, normalizedCompanyName, normalizedApplicant, normalize(remark));
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
        
        sendReviewNotification(normalizedCompanyId, normalizedCompanyName, approved, normalizedReviewer, finalRemark);
        
        return data;
    }
    
    private void sendReviewNotification(String companyId, String companyName, boolean approved, String reviewer, String remark) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("companyId", companyId);
            body.put("companyName", companyName);
            body.put("actionType", approved ? "COMPANY_AUTH_APPROVED" : "COMPANY_AUTH_REJECTED");
            body.put("actionContent", approved 
                    ? "您的企业认证申请已通过审核，审核人：" + reviewer 
                    : "您的企业认证申请未通过审核，原因：" + remark);
            body.put("sourceModule", "COMPANY_AUTH");
            body.put("sourceId", companyId);
            body.put("operator", reviewer);
            
            alertFeignClient.sendAdminActionNotification(body);
        } catch (Exception ex) {
            // 通知发送失败不影响主流程
        }
    }

    private void initTraceCompany(String companyId, String companyName, Map<String, Object> reviewData) {
        Map<String, Object> body = new HashMap<>();
        body.put("companyId", companyId);
        body.put("name", companyName);
        
        String sql = "SELECT address, contact_phone, email, lat, lng FROM company_auth_ext WHERE company_id = ? LIMIT 1";
        Map<String, Object> extInfo = jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            Map<String, Object> info = new HashMap<>();
            info.put("address", rs.getString("address"));
            info.put("contactPhone", rs.getString("contact_phone"));
            info.put("email", rs.getString("email"));
            info.put("lat", rs.getDouble("lat"));
            info.put("lng", rs.getDouble("lng"));
            return info;
        }, companyId);
        
        if (extInfo != null) {
            body.put("address", extInfo.get("address"));
            body.put("contactPhone", extInfo.get("contactPhone"));
            body.put("email", extInfo.get("email"));
            body.put("lat", extInfo.get("lat"));
            body.put("lng", extInfo.get("lng"));
        }

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
    
    public void saveCompanyExtInfo(String companyId, String address, String contactPhone, String email, Double lat, Double lng) {
        String normalizedCompanyId = normalize(companyId);
        if (normalizedCompanyId.isEmpty()) {
            throw new IllegalArgumentException("companyId 不能为空");
        }
        
        String sql = """
                INSERT INTO company_auth_ext(company_id, address, contact_phone, email, lat, lng)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    address = VALUES(address),
                    contact_phone = VALUES(contact_phone),
                    email = VALUES(email),
                    lat = VALUES(lat),
                    lng = VALUES(lng)
                """;
        jdbcTemplate.update(sql, normalizedCompanyId, normalize(address), normalize(contactPhone), normalize(email), lat, lng);
    }

    public List<Map<String, Object>> pendingList() {
        String sql = """
                SELECT ca.company_id, ca.company_name, ca.review_status,
                       ca.apply_by, ca.apply_time,
                       ca.review_by, ca.review_time, ca.remark,
                       cae.address, cae.contact_phone, cae.email, cae.lat, cae.lng
                FROM company_auth ca
                LEFT JOIN company_auth_ext cae ON ca.company_id = cae.company_id
                WHERE ca.review_status = 0
                ORDER BY ca.updated_at DESC
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
            data.put("address", rs.getString("address"));
            data.put("contactPhone", rs.getString("contact_phone"));
            data.put("email", rs.getString("email"));
            data.put("lat", rs.getDouble("lat"));
            data.put("lng", rs.getDouble("lng"));
            return data;
        });
    }

    public String findCompanyIdByUsername(String username) {
        String normalizedUsername = normalize(username);
        if (normalizedUsername.isEmpty()) {
            return "";
        }
        String sql = "SELECT company_id, role FROM auth_user WHERE username = ? AND deleted = 0 LIMIT 1";
        Map<String, String> userRow = jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            Map<String, String> item = new HashMap<>();
            item.put("companyId", rs.getString("company_id"));
            item.put("role", rs.getString("role"));
            return item;
        }, normalizedUsername);
        if (userRow == null) {
            return "";
        }

        String companyId = normalize(userRow.get("companyId"));
        if (!companyId.isEmpty()) {
            return companyId;
        }

        String role = normalize(userRow.get("role")).toUpperCase();
        if (!"COMPANY".equals(role)) {
            return "";
        }

        String generatedCompanyId = generateCompanyId();
        jdbcTemplate.update(
                "UPDATE auth_user SET company_id = ? WHERE username = ? AND deleted = 0 AND (company_id IS NULL OR company_id = '')",
                generatedCompanyId,
                normalizedUsername
        );
        return generatedCompanyId;
    }

    private String generateCompanyId() {
        for (int i = 0; i < 10; i++) {
            String candidate = "C" + LocalDateTime.now().format(COMPANY_ID_FORMATTER)
                    + ThreadLocalRandom.current().nextInt(10, 100);
            Integer existed = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM auth_user WHERE company_id = ?",
                    Integer.class,
                    candidate
            );
            if (existed != null && existed == 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("生成 companyId 失败，请重试");
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

    public Map<String, Object> updateCompanyInfo(String companyId, String companyName) {
        String normalizedCompanyId = normalize(companyId);
        String normalizedCompanyName = normalize(companyName);
        if (normalizedCompanyId.isEmpty() || normalizedCompanyName.isEmpty()) {
            throw new IllegalArgumentException("companyId 和 companyName 不能为空");
        }

        String sql = """
                UPDATE company_auth
                SET company_name = ?, updated_at = NOW()
                WHERE company_id = ?
                """;
        jdbcTemplate.update(sql, normalizedCompanyName, normalizedCompanyId);
        return queryStatus(normalizedCompanyId);
    }
}
