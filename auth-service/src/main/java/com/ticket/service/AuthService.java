package com.ticket.service;

import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.dto.RegisterRequest;
import com.ticket.dto.RegisterResponse;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import com.ticket.security.JwtUtil;
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
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    /**
     * Login - Authenticate existing user
     */
    public LoginResponse login(LoginRequest request) {
        // Find user
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check active
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        //   FIXED: Changed from user.getRole().getRoleName() to user.getRole().name()
        String token = jwtUtil.generateToken(
                user.getUserId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name() //   UserRole enum .name() returns "ADMIN", "SUPPORT_AGENT", etc.
        );
        
        //   FIXED: Changed from user.getRole().getRoleName() to user.getRole().name()
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(), //   UserRole enum .name()
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
}
