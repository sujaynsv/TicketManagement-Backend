package com.assignment.entity;

public enum AgentStatus {
    AVAILABLE,     // Ready to take tickets
    BUSY,          // Has many tickets but can still take more
    OFFLINE        // Not available
}
