package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatusRequest(
    @NotBlank(message = "Status is required")
    String status, // OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REOPENED
    
    String comment // Optional: reason for status change
) {}
