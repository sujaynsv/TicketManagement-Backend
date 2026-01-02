package com.notification.service;

import com.notification.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
public class UserService {
    
    @Value("${auth-service.url}")
    private String authServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get user details from auth-service
     */
    public Optional<UserDTO> getUserById(String userId) {
        try {
            String url = authServiceUrl + "/users/" + userId;
            UserDTO user = restTemplate.getForObject(url, UserDTO.class);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Failed to fetch user {} from auth-service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get user email from auth-service
     */
    public Optional<String> getUserEmail(String userId) {
        return getUserById(userId).map(UserDTO::getEmail);
    }
    
    /**
     * Get user details with fallback
     */
    public UserDTO getUserWithFallback(String userId, String username) {
        Optional<UserDTO> userOpt = getUserById(userId);
        
        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            // Fallback: create minimal user object
            UserDTO fallbackUser = new UserDTO();
            fallbackUser.setUserId(userId);
            fallbackUser.setUsername(username);
            fallbackUser.setEmail(username + "@example.com"); // Fallback email
            return fallbackUser;
        }
    }
}
