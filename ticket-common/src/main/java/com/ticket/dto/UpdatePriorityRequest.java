package com.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriorityRequest {
    
    @NotBlank(message = "Priority is required")
    private String priority;  // CRITICAL, HIGH, MEDIUM, LOW
    
    private String reason;  // Optional: Why this priority was set
}
