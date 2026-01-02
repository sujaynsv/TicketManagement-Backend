package com.assignment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AgentWorkloadDetailsDTO(
        String agentId,
        String agentUsername,
        Integer activeTickets,
        Integer totalAssignedTickets,
        Integer completedTickets,
        String status,
        LocalDateTime lastAssignedAt,
        long totalAssignmentsCount,
        long completedAssignmentsCount,
        List<AdminAssignmentDTO> activeAssignments
) {}
