package com.ticket.service;

import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.dto.LogoutResponse;
import com.ticket.dto.RegisterRequest;
import com.ticket.dto.RegisterResponse;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import com.ticket.security.JwtUtil;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    /**
     * Login - Authenticate existing user
     */
    /**
 * Login - Generate token with tokenVersion
 */
@Transactional
public LoginResponse login(LoginRequest request) {
    // Find user
    User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new RuntimeException("Invalid username or password"));
    
    // Check if user is active
    if (Boolean.FALSE.equals(user.getIsActive())) {
        throw new RuntimeException("Account is deactivated. Please contact administrator.");
    }
    
    // Verify password
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new RuntimeException("Invalid username or password");
    }
    
    // Update last login
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);
    
    //   Get role name from enum
    String roleName = user.getRole().name();  // Returns "ADMIN", "SUPPORT_AGENT", etc.
    
    String token = jwtUtil.generateToken(
            user.getUserId().toString(),
            user.getUsername(),
            user.getEmail(),
            roleName,
            user.getTokenVersion()
    );
    
    log.info("User logged in successfully: {} (tokenVersion: {})", user.getUsername(), user.getTokenVersion());
    
    return new LoginResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            roleName,
            user.getUserId().toString()
    );
}

    
    /**
     * Register - Anyone can register with any role (simplified)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Validate username not taken
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Validate email not taken
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }
        
        //   FIXED: Get UserRole enum from string
        UserRole role = UserRole.fromString(request.roleName()); // Returns END_USER if null
        
        // Create new user
        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setFirstName(request.firstName());
        newUser.setLastName(request.lastName());
        newUser.setRole(role); //   Set UserRole enum directly
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        // Save user
        userRepository.save(newUser);
        
        //   FIXED: Changed from newUser.getRole().getRoleName() to newUser.getRole().name()
        return new RegisterResponse(
                newUser.getUserId().toString(),
                newUser.getUsername(),
                newUser.getEmail(),
                newUser.getRole().name(), //   UserRole enum .name()
                "Registration successful!"
        );
    }

        /**
     * Logout - Invalidate all user's tokens by incrementing tokenVersion
     */
    @Transactional
    public LogoutResponse logout(String userId) {
        UUID userUuid=UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        //   Increment token version (invalidates all existing tokens)
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        
        log.info("User logged out successfully: {} (new tokenVersion: {})", 
                user.getUsername(), user.getTokenVersion());
        
        return new LogoutResponse(
                "Logged out successfully. All sessions invalidated.",
                user.getUsername(),
                LocalDateTime.now()
        );
    }

    /**
 * Validate token and check version against database
 */
    public boolean validateTokenWithVersion(String token) {
        try {
            // Extract user ID from token
            String userId = jwtUtil.extractUserId(token);
            
            // Check if token is expired
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("Token expired for user: {}", userId);
                return false;
            }
            
            // Extract token version from token
            Integer tokenVersion = jwtUtil.extractTokenVersion(token);
            
            // Get user from database
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findById(userUuid)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if token version matches
            if (!tokenVersion.equals(user.getTokenVersion())) {
                log.warn("Token version mismatch for user {}: token={}, db={}", 
                        user.getUsername(), tokenVersion, user.getTokenVersion());
                return false;
            }
            
            log.debug("Token validated successfully for user: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }


}
