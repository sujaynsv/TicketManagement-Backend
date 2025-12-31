package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class TicketAssignedEvent implements Serializable {
    
    private String ticketId;
    private String ticketNumber;
    private String assignedToUserId;
    private String assignedToUsername;
    private String assignedBy;           // Who assigned (manager ID or "SYSTEM")
    private String assignedByUsername;   // Manager name or "AutoAssignment"
    private String assignmentType;       // MANUAL, AUTO
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime assignedAt;
    
    // Constructors
    public TicketAssignedEvent() {}
    
    public TicketAssignedEvent(String ticketId, String ticketNumber, 
                              String assignedToUserId, String assignedToUsername,
                              String assignedBy, String assignedByUsername,
                              String assignmentType, LocalDateTime assignedAt) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.assignedToUserId = assignedToUserId;
        this.assignedToUsername = assignedToUsername;
        this.assignedBy = assignedBy;
        this.assignedByUsername = assignedByUsername;
        this.assignmentType = assignmentType;
        this.assignedAt = assignedAt;
    }
    
    // Getters and Setters
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getTicketNumber() {
        return ticketNumber;
    }
    
    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }
    
    public String getAssignedToUserId() {
        return assignedToUserId;
    }
    
    public void setAssignedToUserId(String assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }
    
    public String getAssignedToUsername() {
        return assignedToUsername;
    }
    
    public void setAssignedToUsername(String assignedToUsername) {
        this.assignedToUsername = assignedToUsername;
    }
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public String getAssignedByUsername() {
        return assignedByUsername;
    }
    
    public void setAssignedByUsername(String assignedByUsername) {
        this.assignedByUsername = assignedByUsername;
    }
    
    public String getAssignmentType() {
        return assignmentType;
    }
    
    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
