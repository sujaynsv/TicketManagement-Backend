package com.assignment.dto;

import jakarta.validation.constraints.NotBlank;

public record BulkReassignRequest(
        @NotBlank(message = "From agent ID is required")
        String fromAgentId,
        
        @NotBlank(message = "To agent ID is required")
        String toAgentId,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
