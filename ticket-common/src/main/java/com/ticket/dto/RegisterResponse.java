package com.ticket.dto;

public record RegisterResponse(
    String userId,
    String username,
    String email,
    String role,
    String message
) {}
