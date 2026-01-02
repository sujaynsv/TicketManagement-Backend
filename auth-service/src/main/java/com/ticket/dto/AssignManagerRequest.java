package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignManagerRequest(
        @NotBlank(message = "Manager ID is required")
        String managerId
) {}
