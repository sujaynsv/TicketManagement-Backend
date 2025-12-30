package com.ticket.enums;

public enum TicketCategory {
    TECHNICAL_ISSUE("Technical Issue", "Hardware or software problems"),
    ACCOUNT_ACCESS("Account & Access", "Login, password, permissions"),
    BILLING("Billing & Payments", "Invoices, payments, refunds"),
    FEATURE_REQUEST("Feature Request", "New feature suggestions"),
    BUG_REPORT("Bug Report", "Software bugs and errors"),
    NETWORK_ISSUE("Network Issue", "Connectivity problems"),
    PERFORMANCE("Performance", "Slow systems or timeouts"),
    DATA_REQUEST("Data Request", "Reports, exports, data queries"),
    TRAINING("Training & Support", "How-to questions, guidance"),
    OTHERS("Others", "General inquiries");
    
    private final String displayName;
    private final String description;
    
    TicketCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static TicketCategory fromString(String category) {
        if (category == null || category.isEmpty()) {
            return OTHERS; // Default
        }
        try {
            return TicketCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHERS; // Fallback to OTHERS if invalid
        }
    }
}
