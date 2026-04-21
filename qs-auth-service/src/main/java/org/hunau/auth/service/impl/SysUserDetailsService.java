package org.hunau.auth.service.impl;

import org.hunau.auth.details.SysUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public SysUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        String sql = """
                SELECT username, phone, email, password_hash, role, company_id, status
                FROM auth_user
                WHERE (username = ? OR phone = ?) AND deleted = 0
                LIMIT 1
                """;

        List<SysUserDetails> users = jdbcTemplate.query(sql, (rs, rowNum) -> {
            SysUserDetails details = new SysUserDetails();
            details.setUsername(rs.getString("username"));
            details.setPassword(rs.getString("password_hash"));
            details.setEmail(rs.getString("email"));
            details.setRole(rs.getString("role"));
            details.setCompanyId(rs.getString("company_id"));
            details.setEnabled(rs.getInt("status") == 1);
            return details;
        }, loginId, loginId);

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return users.get(0);
    }
}