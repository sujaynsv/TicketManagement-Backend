package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:3000"
        ));
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-User-Id",
            "X-Username",
            "X-Role"
        ));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
