package org.hunau.auth.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanyAuthService {

    private final JdbcTemplate jdbcTemplate;

    public CompanyAuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isApproved(String companyId) {
        String sql = "SELECT review_status FROM company_auth WHERE company_id = ? LIMIT 1";
        Integer status = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getInt(1) : null, companyId);
        return status != null && status == 1;
    }

    public boolean review(String companyId, boolean approved) {
        int reviewStatus = approved ? 1 : 0;
        String sql = """
                INSERT INTO company_auth(company_id, company_name, review_status, review_by, review_time, remark)
                VALUES (?, ?, ?, 'admin001', NOW(), ?)
                ON DUPLICATE KEY UPDATE
                    review_status = VALUES(review_status),
                    review_by = VALUES(review_by),
                    review_time = VALUES(review_time),
                    remark = VALUES(remark)
                """;
        String remark = approved ? "review approved" : "review rejected";
        jdbcTemplate.update(sql, companyId, companyId, reviewStatus, remark);
        return approved;
    }
}

