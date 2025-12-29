package com.ticket.controller;

import com.ticket.dto.ErrorResponse;
import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    e.getMessage(),
                    "/auth/login"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running, Testing to check..");
    }
}
