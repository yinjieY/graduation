package org.hunau.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "yourSecretKey32ByteLongForSecurity";
    private static final long EXPIRATION = 1000 * 60 * 60 * 24;

    private static Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 兼容旧调用：仅用户名和角色
    public static String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    // 新调用：附带 companyId（企业用户场景）
    public static String generateToken(String username, String role, String companyId) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey(), SignatureAlgorithm.HS256);
        if (companyId != null && !companyId.isBlank()) {
            builder.claim("companyId", companyId);
        }
        return builder.compact();
    }

    public static String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public static String getCompanyId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("companyId", String.class);
    }

    public static boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}