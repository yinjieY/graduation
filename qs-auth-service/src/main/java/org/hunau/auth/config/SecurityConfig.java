package org.hunau.auth.config;

import org.hunau.auth.service.impl.SysUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

// 使用@EnableWebSecurity注解开启Spring Security功能
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SysUserDetailsService userDetailsService;

    public SecurityConfig(SysUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/auth/login"),
//                                new AntPathRequestMatcher("/auth/register/admin"),
//                                new AntPathRequestMatcher("/register/admin"),
//                                new AntPathRequestMatcher("/auth/register/user"),
//                                new AntPathRequestMatcher("/register/user"),
                                new AntPathRequestMatcher("/auth/register/**"),
                                new AntPathRequestMatcher("/auth/company/**"),
                                new AntPathRequestMatcher("/register/**"),
                                new AntPathRequestMatcher("/scan/**")
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
