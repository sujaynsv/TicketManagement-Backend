package com.ticket.enums;

public enum TicketPriority {
    CRITICAL("Critical", 0, "System down, all users affected", 15, 120),
    HIGH("High", 1, "Major feature broken", 60, 480),
    MEDIUM("Medium", 2, "Feature not working as expected", 240, 1440),
    LOW("Low", 3, "Minor issue or feature request", 720, 2880);
    
    private final String displayName;
    private final int level;
    private final String description;
    private final int responseTimeMinutes;
    private final int resolutionTimeMinutes;
    
    TicketPriority(String displayName, int level, String description, 
                   int responseTimeMinutes, int resolutionTimeMinutes) {
        this.displayName = displayName;
        this.level = level;
        this.description = description;
        this.responseTimeMinutes = responseTimeMinutes;
        this.resolutionTimeMinutes = resolutionTimeMinutes;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getResponseTimeMinutes() {
        return responseTimeMinutes;
    }
    
    public int getResolutionTimeMinutes() {
        return resolutionTimeMinutes;
    }
    
    public boolean isHigherThan(TicketPriority other) {
        return this.level < other.level; // Lower level = higher priority
    }
    
    public static TicketPriority fromString(String priority) {
        if (priority == null || priority.isEmpty()) {
            return MEDIUM; // Default
        }
        try {
            return TicketPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM; // Fallback
        }
    }
}
