package org.hunau.auth.controller;

import org.hunau.auth.details.SysUserDetails;
import org.hunau.common.R;
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
    public R<String> login(@RequestParam String username,
                           @RequestParam String password) {
        SysUserDetails user = (SysUserDetails) userDetailsService.loadUserByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return R.fail("密码错误");
        }

        String token = JwtUtil.generateToken(username, user.getRole());
        return R.ok(token);
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