package com.ticket.dto;

import java.time.LocalDateTime;

public record UserDTO (
    String userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String role,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime lastLogin
){}
