package com.assignment.dto;

import jakarta.validation.constraints.NotBlank;

public record UnassignRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {}
