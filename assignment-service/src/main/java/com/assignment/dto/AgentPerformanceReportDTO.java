package com.assignment.dto;

import java.util.List;

public record AgentPerformanceReportDTO(
        List<AgentPerformanceDTO> agents,
        double avgActiveTickets,
        double avgCompletedTickets,
        double avgSlaCompliance
) {}
