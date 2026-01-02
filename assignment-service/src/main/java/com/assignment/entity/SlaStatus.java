package com.assignment.entity;

public enum SlaStatus {
    ON_TIME,       // Within SLA, plenty of time
    WARNING,       // 80% of SLA time consumed
    BREACHED,      // SLA violated
    MET,           // SLA was met (ticket resolved on time)
    PAUSED         // SLA timer paused
}
