package com.ticket.dto;

public record UserStatsDTO(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long adminCount,
        long managerCount,
        long agentCount,
        long endUserCount
) {}
