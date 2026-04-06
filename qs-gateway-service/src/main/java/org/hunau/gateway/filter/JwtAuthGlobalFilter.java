package org.hunau.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hunau.gateway.config.GatewayAuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> BUILTIN_PUBLIC_PATHS = Set.of(
            "/auth/**",
            "/register/**",
            "/trace/scan/**",
            "/trace/query/**",
            "/trace/qs/image/**",
            "/scan/report",
            "/scan/logs/**"
    );

    private final GatewayAuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(GatewayAuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!isTokenValid(token)) {
            return unauthorized(exchange.getResponse(), "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : BUILTIN_PUBLIC_PATHS) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }

        List<String> whitelist = authProperties.getAuthWhitelist();
        for (String pattern : whitelist) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTokenValid(String token) {
        try {
            String secret = authProperties.getJwtSecret();
            if (!StringUtils.hasText(secret)) {
                return false;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            byte[] expectedSignature = hmacSha256((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8),
                    secret.getBytes(StandardCharsets.UTF_8));
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<?, ?> claims = objectMapper.readValue(payloadBytes, Map.class);
            Object exp = claims.get("exp");
            if (!(exp instanceof Number)) {
                return false;
            }

            long now = Instant.now().getEpochSecond();
            return ((Number) exp).longValue() > now;
        } catch (Exception ex) {
            return false;
        }
    }

    private byte[] hmacSha256(byte[] data, byte[] secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("code", HttpStatus.UNAUTHORIZED.value());
        body.put("msg", message);

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            payload = "{\"code\":401,\"msg\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)));
    }
}

