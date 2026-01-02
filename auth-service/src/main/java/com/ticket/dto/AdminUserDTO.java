package com.ticket.dto;

import java.time.LocalDateTime;

public record AdminUserDTO(
        String userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String role,
        Boolean isActive,
        String managerId,
        String managerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin
) {}
