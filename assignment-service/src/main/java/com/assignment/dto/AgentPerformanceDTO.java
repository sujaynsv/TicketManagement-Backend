package com.assignment.dto;

import java.time.LocalDateTime;

public record AgentPerformanceDTO(
        String agentId,
        String agentUsername,
        int activeTickets,
        int completedTickets,
        long totalAssignments,
        long activeAssignments,
        long completedAssignments,
        String avgResolutionTime,
        double slaComplianceRate,
        String status,
        LocalDateTime lastAssignedAt
) {}
