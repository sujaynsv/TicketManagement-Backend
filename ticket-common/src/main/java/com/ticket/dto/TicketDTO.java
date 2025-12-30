package com.ticket.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDTO(
    String ticketId,
    String ticketNumber,
    String title,
    String description,
    String status,
    String category,
    String priority,
    String createdByUserId,
    String createdByUsername,
    String assignedToUserId,
    String assignedToUsername,
    List<String> tags,
    Integer commentCount,
    Integer attachmentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime assignedAt,
    LocalDateTime resolvedAt,
    LocalDateTime closedAt
) {}
