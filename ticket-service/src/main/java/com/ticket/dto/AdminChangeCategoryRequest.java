package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminChangeCategoryRequest(
        @NotBlank(message = "Category is required")
        String category,
        
        @NotBlank(message = "Reason is required")
        String reason
) {}
