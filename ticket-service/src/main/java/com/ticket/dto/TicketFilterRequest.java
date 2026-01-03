package com.ticket.dto;

/**
 * DTO to encapsulate ticket filter parameters
 * Solves "too many parameters" issue
 */
public record TicketFilterRequest(
    int page,
    int size,
    String status,
    String priority,
    String category,
    String assignedToUserId,
    String createdByUserId,
    String search
) {
    public TicketFilterRequest {
        // Validation
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
    }
}
