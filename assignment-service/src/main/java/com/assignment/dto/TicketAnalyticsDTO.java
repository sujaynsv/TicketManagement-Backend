package com.assignment.dto;

import java.time.LocalDate;
import java.util.Map;

public record TicketAnalyticsDTO(
        Map<LocalDate, Long> createdByDay,
        Map<LocalDate, Long> resolvedByDay,
        Map<String, Long> byPriority,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory
) {}
