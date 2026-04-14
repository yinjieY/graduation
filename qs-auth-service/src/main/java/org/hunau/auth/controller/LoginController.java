package org.hunau.auth.controller;

import org.hunau.auth.details.SysUserDetails;
import org.hunau.common.model.R;
import org.hunau.common.util.JwtUtil;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
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