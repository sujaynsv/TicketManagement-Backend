package com.assignment.dto;

import java.util.Map;

public record CategoryBreakdownDTO(
        Map<String, Long> categoryCounts,
        Map<String, Double> categoryPercentages,
        long totalTickets
) {}
