package com.ticket.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ticket_activity")
public class TicketActivity {
    
    @Id
    private String activityId;
    
    private String ticketId;
    
    private String activityType; // STATUS_CHANGED, COMMENT_ADDED, ASSIGNED, ATTACHMENT_UPLOADED
    
    private String description; // "Status changed from OPEN to IN_PROGRESS"
    
    private String performedByUserId;
    
    private String performedByUsername;
    
    private String oldValue; // Optional: previous status
    
    private String newValue; // Optional: new status
    
    private LocalDateTime createdAt;
    
    // Constructors
    public TicketActivity() {}
    
    public TicketActivity(String ticketId, String activityType, String description, 
                         String performedByUserId, String performedByUsername) {
        this.ticketId = ticketId;
        this.activityType = activityType;
        this.description = description;
        this.performedByUserId = performedByUserId;
        this.performedByUsername = performedByUsername;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getActivityId() {
        return activityId;
    }
    
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getActivityType() {
        return activityType;
    }
    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPerformedByUserId() {
        return performedByUserId;
    }
    
    public void setPerformedByUserId(String performedByUserId) {
        this.performedByUserId = performedByUserId;
    }
    
    public String getPerformedByUsername() {
        return performedByUsername;
    }
    
    public void setPerformedByUsername(String performedByUsername) {
        this.performedByUsername = performedByUsername;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
