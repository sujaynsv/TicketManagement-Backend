package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminChangePriorityRequest(
        @NotBlank(message = "Priority is required")
        String priority,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
