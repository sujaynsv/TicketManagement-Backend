package com.ticket.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTicketRequest(
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    String title,
    
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    String description,
    
    String category,
    
    String priority,
    
    List<String> tags
) {}
