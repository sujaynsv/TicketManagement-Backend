package com.assignment.entity;

public enum SlaStatus {
    OK,            // Within SLA
    WARNING,       // 80% of SLA time consumed
    BREACHED,      // SLA violated
    PAUSED         // SLA timer paused
}
