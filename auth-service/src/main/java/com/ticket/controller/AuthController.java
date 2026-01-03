package com.ticket.controller;

import com.ticket.dto.ErrorResponse;
import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.dto.LogoutResponse;
import com.ticket.dto.RegisterRequest;
import com.ticket.dto.RegisterResponse;
import com.ticket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private AuthService authService;

    public AuthController(AuthService authService){
        this.authService=authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // You may want to handle errors differently, but for now, return null or throw the exception
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // You may want to handle errors differently, but for now, return null or throw the exception
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running, Testing to check..");
    }


    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            boolean isValid = authService.validateTokenWithVersion(token);

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "Token is valid" : "Token is invalid or expired"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valid", false,
                    "message", "Token validation failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("X-User-Id") String userId) {
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
