package com.ticket.dto;

import java.time.LocalDateTime;

public record CommentDTO(
    String commentId,
    String ticketId,
    String userId,
    String username,
    String commentText,
    Boolean isInternal,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
