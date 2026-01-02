package com.assignment.dto;

public record AssignmentStatsDTO(
        long totalAssignments,
        long activeAssignments,
        long reassignedCount,
        long notAssignedCount,
        long autoAssignments,
        long manualAssignments,
        long unassignedTickets,
        long totalAgents,
        long availableAgents,
        long busyAgents,
        long offlineAgents
) {}
