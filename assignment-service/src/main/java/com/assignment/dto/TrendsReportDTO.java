package com.assignment.dto;

import java.util.Map;

public record TrendsReportDTO(
        String period,
        int days,
        Map<String, Long> ticketsByPeriod,
        Map<String, Long> assignmentsByPeriod,
        Map<String, Long> resolutionsByPeriod
) {}
