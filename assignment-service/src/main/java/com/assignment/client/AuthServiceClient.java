package com.assignment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${auth-service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    public AuthServiceClient() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Get all agents from Auth Service
     */
    public List<AgentDTO> getAllAgents() {
        try {
            String url = authServiceUrl + "/users/agents";
            
            ResponseEntity<List<AgentDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AgentDTO>>() {}
            );
            
            List<AgentDTO> agents = response.getBody();
            log.info("Fetched {} agents from Auth Service", agents != null ? agents.size() : 0);
            
            return agents != null ? agents : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Failed to fetch agents from Auth Service: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get user by ID from Auth Service
     */
    public AgentDTO getUserById(String userId) {
        try {
            String url = authServiceUrl + "/users/" + userId;
            
            ResponseEntity<AgentDTO> response = restTemplate.getForEntity(url, AgentDTO.class);
            
            log.info("Fetched user {} from Auth Service", userId);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Failed to fetch user {} from Auth Service: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * DTO for Agent data from Auth Service
     */
    public static class AgentDTO {
        private String userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private Boolean isActive;
        
        // Constructors
        public AgentDTO() {}
        
        // Getters and Setters
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public Boolean getIsActive() {
            return isActive;
        }
        
        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }
}
