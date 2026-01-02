package com.assignment.dto;

import java.time.LocalDateTime;

public record AdminAssignmentDTO(
        String assignmentId,
        String ticketId,
        String ticketNumber,
        String ticketTitle,
        String priority,
        String category,
        String agentId,
        String agentUsername,
        String assignedBy,
        String assignedByUsername,
        String assignmentType,
        String assignmentStrategy,
        String previousAgentId,
        String previousAgentUsername,
        String reassignmentReason,
        String assignmentNotes,
        String status,
        String ticketStatus,
        LocalDateTime assignedAt,
        LocalDateTime completedAt
) {}
