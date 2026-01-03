package com.ticket.controller;

import com.ticket.dto.ErrorResponse;
import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.dto.LogoutResponse;
import com.ticket.dto.RegisterRequest;
import com.ticket.dto.RegisterResponse;
import com.ticket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

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

        @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Registration Failed",
                    e.getMessage(),
                    "/auth/register"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running, Testing to check..");
    }


    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            boolean isValid = authService.validateTokenWithVersion(token);
            
            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "Token is valid" : "Token is invalid or expired"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Token validation failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-User-Id") String userId) {
        try {
            LogoutResponse response = authService.logout(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    "Logout Failed",
                    e.getMessage(),
                    "/auth/logout"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }


}
