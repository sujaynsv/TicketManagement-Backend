package com.assignment.dto;

import java.util.List;

public record SystemOverviewDTO(
        long totalTickets,
        long activeTickets,
        long resolvedToday,
        String avgResolutionTime,
        double slaComplianceRate,
        long totalAgents,
        long activeAgents,
        long busyAgents,
        List<CategoryCount> topCategories,
        long criticalTicketsOpen,
        long recentAssignments
) {}
