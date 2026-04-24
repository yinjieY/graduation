package org.hunau.auth.controller;

import org.hunau.auth.details.SysUserDetails;
import org.hunau.common.model.R;
import org.hunau.common.util.JwtUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public LoginController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public R<String> login(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String account,
                           @RequestParam String password) {
        String loginId = normalize(account);
        if (loginId.isEmpty()) {
            loginId = normalize(username);
        }
        if (loginId.isEmpty()) {
            return R.fail("用户名或手机号不能为空");
        }

        SysUserDetails user = (SysUserDetails) userDetailsService.loadUserByUsername(loginId);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return R.fail("密码错误");
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getRole(), user.getCompanyId());
        return R.ok(token);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
    
    @GetMapping("/user/info")
    public R<Map<String, Object>> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return R.fail("未登录");
        }
        
        String username = authentication.getName();
        String sql = "SELECT username, phone, email, role, company_id FROM auth_user WHERE username = ? AND deleted = 0 LIMIT 1";
        
        Map<String, Object> userInfo = jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            Map<String, Object> info = new HashMap<>();
            info.put("username", rs.getString("username"));
            info.put("phone", rs.getString("phone"));
            info.put("email", rs.getString("email"));
            info.put("role", rs.getString("role"));
            info.put("companyId", rs.getString("company_id"));
            return info;
        }, username);
        
        if (userInfo == null) {
            return R.fail("用户不存在");
        }
        
        return R.ok(userInfo);
    }
    
    @PostMapping("/user/update")
    public R<Map<String, Object>> updateUserInfo(@RequestParam(required = false) String email,
                                                 @RequestParam(required = false) String phone) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return R.fail("未登录");
        }
        
        String username = authentication.getName();
        
        if (isBlank(email) && isBlank(phone)) {
            return R.fail("至少需要修改邮箱或手机号");
        }
        
        StringBuilder sql = new StringBuilder("UPDATE auth_user SET ");
        Map<String, Object> params = new HashMap<>();
        
        if (!isBlank(email)) {
            sql.append("email = ?, ");
            params.put("email", email.trim());
        }
        if (!isBlank(phone)) {
            sql.append("phone = ?, ");
            params.put("phone", phone.trim());
        }
        
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE username = ? AND deleted = 0");
        
        jdbcTemplate.update(sql.toString(), 
                !isBlank(email) ? email.trim() : null,
                !isBlank(phone) ? phone.trim() : null,
                username);
        
        return getUserInfo();
    }
    
    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

/*

// 仅管理员可访问
@PreAuthorize("hasRole('ADMIN')")

// 仅企业可访问
@PreAuthorize("hasRole('COMPANY')")

// 仅消费者可访问
@PreAuthorize("hasRole('CONSUMER')")

// 管理员 + 企业均可访问
@PreAuthorize("hasAnyRole('ADMIN','COMPANY')")

 */