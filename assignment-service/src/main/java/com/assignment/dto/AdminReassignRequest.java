package com.assignment.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminReassignRequest(
        @NotBlank(message = "New agent ID is required")
        String newAgentId,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
