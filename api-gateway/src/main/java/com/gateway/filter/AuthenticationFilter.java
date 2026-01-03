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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final WebClient webClient;
    
    /**
     * Complete Role-Based Access Control Map
     */
    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            
            // =========================================
            // END_USER - Can only manage own tickets
            // =========================================
            "END_USER", Arrays.asList(
                    // Ticket Management
                    "POST:/auth/logout",
                    "POST:/tickets",                          // Create ticket
                    "GET:/tickets/my",                        // View my tickets
                    "GET:/tickets/number/",                   // View ticket by number
                    "GET:/tickets/",                          // View specific ticket (by ID)
                    "PATCH:/tickets/",                        // Update own ticket
                    "PUT:/tickets/",                          // Update own ticket
                    
                    // Comments
                    "POST:/tickets/",                         // Add comment to ticket
                    "GET:/tickets/",                          // View comments
                    
                    // Attachments
                    "POST:/tickets/",                         // Upload attachment
                    "GET:/tickets/",                          // View attachments
                    
                    // Activities
                    "GET:/tickets/",                          // View ticket activities
                    
                    // Notifications
                    "GET:/notifications/users/",              // View own notifications
                    "PATCH:/notifications/",                  // Mark as read
                    
                    // User Info
                    "GET:/users/"                             // View user info
            ),
            
            // =========================================
            // SUPPORT_AGENT - Can work on assigned tickets
            // =========================================
            "SUPPORT_AGENT", Arrays.asList(
                    // Ticket Management
                    "POST:/auth/logout",
                    "GET:/tickets",                           // View all tickets
                    "GET:/tickets/",                          // View any ticket
                    "GET:/tickets/assigned",                  // View assigned tickets
                    "GET:/tickets/my",                        // View created tickets
                    "GET:/tickets/status/",                   // View by status
                    "GET:/tickets/number/",                   // View by number
                    "PUT:/tickets/",                          // Update tickets
                    "PATCH:/tickets/",                        // Change status/priority
                    
                    // Comments
                    "POST:/tickets/",                         // Add comments (including internal)
                    "GET:/tickets/",                          // View comments
                    
                    // Attachments
                    "POST:/tickets/",                         // Upload attachments
                    "GET:/tickets/",                          // View attachments
                    "DELETE:/tickets/",                       // Delete attachments
                    
                    // Activities
                    "GET:/tickets/",                          // View activities
                    
                    // Assignment
                    "GET:/assignments/ticket/",               // View assignment info
                    "PUT:/assignments/",                      // Accept assignment
                    
                    // Agents
                    "GET:/agents",                            // View agents
                    "GET:/agents/",                           // View agent info
                    "PUT:/agents/",                           // Update own status
                    
                    // SLA
                    "GET:/sla/tickets/",                      // View SLA info
                    "GET:/sla/active",                        // View active SLAs
                    "GET:/sla/warnings",                      // View warnings
                    
                    // Notifications
                    "GET:/notifications/users/",              // View own notifications
                    "PATCH:/notifications/",                  // Mark as read
                    
                    // Users
                    "GET:/users/agents",                      // View agents
                    "GET:/users/"                             // View user info
            ),
            
            // =========================================
            // SUPPORT_MANAGER - Can manage team & assignments
            // =========================================
            "SUPPORT_MANAGER", Arrays.asList(
                    // Ticket Management (Full Access)
                    "POST:/auth/logout",
                    "GET:/tickets",                           // View all tickets
                    "GET:/tickets/",                          // View any ticket
                    "GET:/tickets/assigned",                  // View assigned
                    "GET:/tickets/my",                        // View own
                    "GET:/tickets/status/",                   // View by status
                    "GET:/tickets/number/",                   // View by number
                    "POST:/tickets",                          // Create tickets
                    "PUT:/tickets/",                          // Update any ticket
                    "PATCH:/tickets/",                        // Change status/priority
                    "DELETE:/tickets/",                       // Delete tickets
                    
                    // Escalation
                    "POST:/tickets/",                         // Escalate tickets
                    
                    // Comments (All)
                    "POST:/tickets/",                         // Add any comment
                    "GET:/tickets/",                          // View all comments
                    
                    // Attachments
                    "POST:/tickets/",                         // Upload
                    "GET:/tickets/",                          // View
                    "DELETE:/tickets/",                       // Delete
                    
                    // Activities
                    "GET:/tickets/",                          // View activities
                    
                    // Assignment Management
                    "POST:/assignments/manual",               //   Manually assign
                    "POST:/assignments/auto",                 // Trigger auto-assign
                    "PUT:/assignments/reassign",              // Reassign tickets
                    "GET:/assignments",                       // View all assignments
                    "GET:/assignments/",                      // View assignment details
                    "GET:/assignments/ticket/",               // View ticket assignment
                    "GET:/assignments/tickets/unassigned",    // View unassigned
                    "GET:/assignments/agents/",               // View agent tickets
                    "GET:/assignments/agents/available",      // View available agents
                    
                    // Agent Management
                    "GET:/agents",                            // View all agents
                    "GET:/agents/",                           // View agent details
                    "PUT:/agents/",                           // Update agent status
                    "POST:/agents/sync",                      // Sync agents
                    
                    // SLA Management
                    "GET:/sla",                               // View all SLA
                    "GET:/sla/",                              // View SLA details
                    "GET:/sla/tickets/",                      // View ticket SLA
                    "GET:/sla/active",                        // View active
                    "GET:/sla/breached",                      // View breached
                    "GET:/sla/warnings",                      // View warnings
                    
                    // Notifications
                    "GET:/notifications/users/",              // View notifications
                    "PATCH:/notifications/",                  // Mark as read
                    
                    // Users
                    "GET:/users/agents",                      // View agents
                    "GET:/users/managers",                    // View managers
                    "GET:/users/"                             // View user info
            ),
            
            // =========================================
            // ADMIN - Full System Access
            // =========================================
            "ADMIN", Arrays.asList(
                    // Universal Access
                    "*",                                      // Full access to everything
                    
                    // Explicitly listed for clarity:
                    // Admin Tickets
                    "POST:/auth/logout",
                    "GET:/admin/tickets",
                    "GET:/admin/tickets/",
                    "PUT:/admin/tickets/",
                    "PATCH:/admin/tickets/",
                    "DELETE:/admin/tickets/",
                    "GET:/admin/tickets/stats",
                    "GET:/admin/tickets/user/",
                    "GET:/admin/tickets/agent/",
                    
                    // Admin Assignments
                    "GET:/admin/assignments",
                    "GET:/admin/assignments/",
                    "PUT:/admin/assignments/",
                    "DELETE:/admin/assignments/",
                    "POST:/admin/assignments/bulk-reassign",
                    "GET:/admin/assignments/stats",
                    "GET:/admin/assignments/agent/",
                    "GET:/admin/assignments/unassigned",
                    "GET:/admin/assignments/ticket/",
                    
                    // Admin Users
                    "GET:/admin/users",
                    "GET:/admin/users/",
                    "POST:/admin/users",
                    "PUT:/admin/users/",
                    "DELETE:/admin/users/",
                    "GET:/admin/users/stats",
                    "GET:/admin/users/agents",
                    "GET:/admin/users/managers",
                    
                    // Admin Analytics
                    "GET:/admin/analytics/overview",
                    "GET:/admin/analytics/tickets",
                    "GET:/admin/analytics/agents",
                    "GET:/admin/analytics/sla",
                    "GET:/admin/analytics/categories",
                    "GET:/admin/analytics/trends"
            )
    );
    
    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            log.info("Authentication filter processing: {} {}", 
                    request.getMethod(), request.getPath());
            
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
                
                // Validate token version with auth-service
                return validateTokenWithAuthService(token)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                log.warn("Token invalidated (logged out) for: {}", request.getPath());
                                return onError(exchange, "Token has been invalidated. Please login again.", 
                                        HttpStatus.UNAUTHORIZED);
                            }
                            
                            // Extract user info
                            String userId = jwtUtil.extractUserId(token);
                            String username = jwtUtil.extractUsername(token);
                            String role = jwtUtil.extractRole(token);
                            
                            log.info("Authenticated: user={}, role={}, path={}", 
                                    username, role, request.getPath());
                            
                            //   Check role-based permissions
                            String path = request.getPath().toString();
                            String method = request.getMethod().toString();
                            
                            if (!hasPermission(role, method, path)) {
                                log.warn("Access denied: {} (role={}) attempted {} {}", 
                                        username, role, method, path);
                                return onError(exchange, 
                                        String.format("Access denied. Your role (%s) does not have permission for this action.", role), 
                                        HttpStatus.FORBIDDEN);
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
     * Check if role has permission for the given method and path
     */
    private boolean hasPermission(String role, String method, String path) {
        List<String> permissions = ROLE_PERMISSIONS.getOrDefault(role, List.of());
        
        // Check for wildcard permission (ADMIN)
        if (permissions.contains("*")) {
            return true;
        }
        
        String requiredPermission = method + ":" + path;
        
        for (String permission : permissions) {
            String[] parts = permission.split(":", 2);
            if (parts.length != 2) continue;
            
            String permMethod = parts[0];
            String permPath = parts[1];
            
            // Method must match (or be wildcard)
            if (!permMethod.equals(method) && !permMethod.equals("*")) {
                continue;
            }
            
            // Exact path match
            if (permPath.equals(path)) {
                return true;
            }
            
            // Prefix match (e.g., "/tickets/" matches "/tickets/123")
            if (permPath.endsWith("/") && path.startsWith(permPath)) {
                return true;
            }
            
            // Pattern match for paths with IDs
            // "/tickets/" matches "/tickets/abc123"
            // "/tickets/" matches "/tickets/abc123/comments"
            if (permPath.endsWith("/")) {
                String basePath = permPath.substring(0, permPath.length() - 1);
                if (path.equals(basePath) || path.startsWith(permPath)) {
                    return true;
                }
            }
        }
        
        log.debug("Permission denied: role={}, required={}", role, requiredPermission);
        return false;
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
        String errorResponse = String.format("{\"error\": \"%s\", \"status\": %d}", 
                error, httpStatus.value());
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes()))
        );
    }
    
    public static class Config {
        // Empty config class
    }
}
