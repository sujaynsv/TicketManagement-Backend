package com.ticket.client;

import com.ticket.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);
    
    @Override
    public UserDTO getUserById(String userId) {
        log.warn("Circuit breaker activated for getUserById({}). Using fallback.", userId);
        
        // Return fallback user data
        UserDTO fallbackUser = new UserDTO();
        fallbackUser.setUserId(userId);
        fallbackUser.setUsername("Unknown User");
        fallbackUser.setEmail("unavailable@example.com");
        fallbackUser.setFirstName("Service");
        fallbackUser.setLastName("Unavailable");
        
        return fallbackUser;
    }
}
