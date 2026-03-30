package org.hunau.auth.service;

import org.hunau.auth.model.RegisterRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class RegisterService {

    private static final Set<String> ALLOWED_ROLES = Set.of("COMPANY", "CONSUMER");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {
        String role = normalize(request.getRole()).toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("仅支持 COMPANY 或 CONSUMER 注册");
        }
        return registerInternal(request, role);
    }

    // Temporary endpoint for local testing: create admin directly.
    public String registerAdmin(RegisterRequest request) {
        return registerInternal(request, "ADMIN");
    }

    private String registerInternal(RegisterRequest request, String role) {
        String username = normalize(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword().trim();
        String companyId = normalize(request.getCompanyId());

        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if ("COMPANY".equals(role) && companyId.isEmpty()) {
            throw new IllegalArgumentException("企业用户必须提供 companyId");
        }

        Integer existed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_user WHERE username = ? AND deleted = 0",
                Integer.class,
                username
        );
        if (existed > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO auth_user(username, password_hash, role, company_id, status, deleted) VALUES (?, ?, ?, ?, 1, 0)",
                username,
                encodedPassword,
                role,
                "COMPANY".equals(role) ? companyId : null
        );

        return username;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

