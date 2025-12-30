package com.ticket.dto;

import java.time.LocalDateTime;

public record TicketActivityDTO(
    String activityId,
    String ticketId,
    String activityType,
    String description,
    String performedByUserId,
    String performedByUsername,
    String oldValue,
    String newValue,
    LocalDateTime createdAt
) {}
