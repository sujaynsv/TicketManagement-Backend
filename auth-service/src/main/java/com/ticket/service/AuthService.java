package com.ticket.service;

import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.entity.User;
import com.ticket.repository.UserRepository;
import com.ticket.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        String token = jwtUtil.generateToken(
                user.getUserId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getRoleName()
        );
        
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().getRoleName(),
                user.getUserId().toString()
        );
    }
}
