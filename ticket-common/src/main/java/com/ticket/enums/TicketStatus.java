package com.ticket.enums;

public enum TicketStatus {
    OPEN("Open", "Newly created ticket"),
    ASSIGNED("Assigned", "Ticket assigned to an agent"),
    IN_PROGRESS("In Progress", "Agent is working on the ticket"),
    RESOLVED("Resolved", "Issue has been resolved"),
    CLOSED("Closed", "Ticket closed and confirmed"),
    REOPENED("Reopened", "Ticket reopened by customer"),
    ESCALATED("Escalated", "Ticket Escalated to Manager");
    
    private final String displayName;
    private final String description;
    
    TicketStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canTransitionTo(TicketStatus newStatus) {
        switch (this) {
            case OPEN:
                return newStatus == ASSIGNED || newStatus == CLOSED;
            case ASSIGNED:
                return newStatus == IN_PROGRESS || newStatus == CLOSED;
            case IN_PROGRESS:
                return newStatus == RESOLVED || newStatus == REOPENED || newStatus == CLOSED;
            case RESOLVED:
                return newStatus == CLOSED || newStatus == REOPENED;
            case CLOSED:
                return newStatus == REOPENED;
            case REOPENED:
                return newStatus == IN_PROGRESS || newStatus == CLOSED;
            case ESCALATED:
                return newStatus == IN_PROGRESS || newStatus==ASSIGNED;
            default:
                return false;
        }
    }
}
