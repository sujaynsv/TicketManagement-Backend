package com.assignment.dto;

import java.time.LocalDateTime;

public class UnassignedTicketDTO {
    
    private String ticketId;
    private String ticketNumber;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private String slaStatus;
    private String timeRemaining;
    
    // Constructors
    public UnassignedTicketDTO() {}
    
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedByUsername() {
        return createdByUsername;
    }
    
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSlaStatus() {
        return slaStatus;
    }
    
    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }
    
    public String getTimeRemaining() {
        return timeRemaining;
    }
    
    public void setTimeRemaining(String timeRemaining) {
        this.timeRemaining = timeRemaining;
    }
}
