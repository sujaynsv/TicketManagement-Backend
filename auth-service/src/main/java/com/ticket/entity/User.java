package com.ticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ticket.enums.UserRole;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;  
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @PrePersist
    protected void onCreate() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
