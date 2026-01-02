package com.assignment.dto;

import java.util.Map;

public record SlaComplianceReportDTO(
        long totalTracked,
        long onTime,
        long breached,
        long warning,
        double complianceRate,
        Map<String, SlaByPriorityDTO> byPriority
) {}
