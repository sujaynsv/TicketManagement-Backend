package com.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        log.info("Incoming Request: {} {}", request.getMethod(), request.getPath());
        log.debug("Request Headers: {}", request.getHeaders());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> 
            log.info("Response Status: {}", exchange.getResponse().getStatusCode())
        ));
    }
    
    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}
