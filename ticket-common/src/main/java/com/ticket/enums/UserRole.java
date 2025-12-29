package com.ticket.enums;

public enum UserRole {
    ADMIN("System Administrator", 4),
    SUPPORT_MANAGER("Support Team Manager", 3),
    SUPPORT_AGENT("Support Team Agent", 2),
    END_USER("End User / Customer", 1);
    
    private final String description;
    private final int level;
    
    UserRole(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(UserRole other) {
        return this.level > other.level;
    }
    
    public boolean isStaff() {
        return this != END_USER;
    }
    
    public static UserRole fromString(String role) {
        if (role == null || role.isEmpty()) {
            return END_USER; // Default
        }
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + 
                ". Valid roles: ADMIN, SUPPORT_MANAGER, SUPPORT_AGENT, END_USER");
        }
    }
}
