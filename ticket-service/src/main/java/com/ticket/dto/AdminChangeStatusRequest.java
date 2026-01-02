package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminChangeStatusRequest(
        @NotBlank(message = "Status is required")
        String status,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
