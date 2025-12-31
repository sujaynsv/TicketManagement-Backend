package com.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignmentRequest {
    
    @NotBlank(message = "Ticket ID is required")
    private String ticketId;
    
    @NotBlank(message = "New agent ID is required")
    private String newAgentId;
    
    private String reassignmentReason;  // Optional
}
