package com.assignment.dto;

public record SlaByPriorityDTO(
        long tracked,
        long breached,
        long responseBreached,
        long resolutionBreached,
        double complianceRate
) {}
