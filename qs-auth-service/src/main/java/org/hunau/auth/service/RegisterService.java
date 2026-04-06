package org.hunau.auth.service;

import org.hunau.auth.model.RegisterRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class RegisterService {

    private static final Set<String> ALLOWED_ROLES = Set.of("COMPANY", "CONSUMER");
    private static final DateTimeFormatter COMPANY_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String role = normalize(request.getRole()).toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("仅支持 COMPANY 或 CONSUMER 注册");
        }
        return registerInternal(request, role);
    }

    // Temporary endpoint for local testing: create admin directly.
    @Transactional
    public Map<String, Object> registerAdmin(RegisterRequest request) {
        return registerInternal(request, "ADMIN");
    }

    private Map<String, Object> registerInternal(RegisterRequest request, String role) {
        String username = normalize(request.getUsername());
        String phone = normalize(request.getPhone());
        String password = request.getPassword() == null ? "" : request.getPassword().trim();

        if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("用户名、手机号和密码不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("手机号格式不正确，应为11位中国大陆手机号");
        }

        Integer existed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_user WHERE username = ? AND deleted = 0",
                Integer.class,
                username
        );
        if (existed > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Integer existedPhone = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_user WHERE phone = ? AND deleted = 0",
                Integer.class,
                phone
        );
        if (existedPhone > 0) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        String companyId = "COMPANY".equals(role) ? generateCompanyId() : null;
        String encodedPassword = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO auth_user(username, phone, password_hash, role, company_id, status, deleted) VALUES (?, ?, ?, ?, ?, 1, 0)",
                username,
                phone,
                encodedPassword,
                role,
                companyId
        );

        String companyName = null;
        if ("COMPANY".equals(role)) {
            companyName = normalize(request.getCompanyName());
            if (companyName.isEmpty()) {
                companyName = username;
            }
            initPendingCompanyAuth(companyId, companyName, request.getRemark(), username);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", username);
        data.put("phone", phone);
        data.put("role", role);
        data.put("companyId", companyId);
        data.put("companyName", companyName);
        data.put("reviewStatus", "COMPANY".equals(role) ? 0 : null);
        data.put("registered", true);
        return data;
    }

    private void initPendingCompanyAuth(String companyId, String companyName, String remark, String applicant) {
        String normalizedRemark = normalize(remark);
        if (normalizedRemark.isEmpty()) {
            normalizedRemark = "register auto created";
        }
        String normalizedApplicant = normalize(applicant);
        if (normalizedApplicant.isEmpty()) {
            normalizedApplicant = "system";
        }
        String sql = """
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
        jdbcTemplate.update(sql, companyId, companyName, normalizedApplicant, normalizedRemark);
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
            if (existed == 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("生成 companyId 失败，请重试");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
