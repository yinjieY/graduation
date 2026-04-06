package org.hunau.alert.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.hunau.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignClientConfig {

    @Value("${app.internal-auth.username:qs-alert-service}")
    private String internalUsername;

    @Value("${app.internal-auth.role:SERVICE}")
    private String internalRole;

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                2000, TimeUnit.MILLISECONDS,
                4000, TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(200, 1000, 2);
    }

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            String authHeader = resolveAuthorizationHeader();
            if (authHeader == null || authHeader.isBlank()) {
                String token = JwtUtil.generateToken(internalUsername, internalRole);
                authHeader = "Bearer " + token;
            }
            template.header("Authorization", authHeader);
        };
    }

    private String resolveAuthorizationHeader() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        return request.getHeader("Authorization");
    }
}

