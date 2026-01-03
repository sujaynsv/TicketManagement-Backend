package com.gateway.filter;

import com.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final WebClient webClient;
    
    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            log.info("Authentication filter processing: {}", request.getPath());
            
            // Check Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for: {}", request.getPath());
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }
            
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format for: {}", request.getPath());
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            
            try {
                // Basic JWT validation (expiration check)
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid or expired token for: {}", request.getPath());
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }
                
                // ✅ Validate token version with auth-service
                return validateTokenWithAuthService(token)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                log.warn("Token invalidated (logged out) for: {}", request.getPath());
                                return onError(exchange, "Token has been invalidated. Please login again.", HttpStatus.UNAUTHORIZED);
                            }
                            
                            // Extract user info
                            String userId = jwtUtil.extractUserId(token);
                            String username = jwtUtil.extractUsername(token);
                            String role = jwtUtil.extractRole(token);
                            
                            log.info("Authenticated: user={}, role={}, path={}", username, role, request.getPath());
                            
                            // Check admin endpoints
                            String path = request.getPath().toString();
                            if (path.contains("/admin/") && !"ADMIN".equals(role)) {
                                log.warn("Access denied: {} (role={}) attempted admin endpoint: {}", username, role, path);
                                return onError(exchange, "Access denied. Admin role required.", HttpStatus.FORBIDDEN);
                            }
                            
                            // Add user info to headers for downstream services
                            ServerHttpRequest modifiedRequest = request.mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-Username", username)
                                    .header("X-Role", role)
                                    .build();
                            
                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        });
                
            } catch (Exception e) {
                log.error("JWT validation error: {}", e.getMessage());
                return onError(exchange, "JWT validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }
    
    /**
     * Validate token with auth-service (checks token version)
     */
    private Mono<Boolean> validateTokenWithAuthService(String token) {
        return webClient.post()
                .uri("http://auth-service/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", token))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean isValid = (Boolean) response.get("valid");
                    return isValid != null && isValid;
                })
                .onErrorResume(e -> {
                    log.error("Error validating token with auth-service: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
    
    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String errorResponse = String.format("{\"error\": \"%s\", \"status\": %d}", error, httpStatus.value());
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes()))
        );
    }
    
    public static class Config {
        // Empty config class
    }
}
