package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TicketCreatedEvent implements Serializable {
    
    private String ticketId;
    private String ticketNumber;
    private String title;
    private String createdByUserId;
    private String createdByUsername;
    private String category;
    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Constructors
    public TicketCreatedEvent() {}
    
    public TicketCreatedEvent(String ticketId, String ticketNumber, String title,
                             String createdByUserId, String createdByUsername,
                             String category, String priority, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.title = title;
        this.createdByUserId = createdByUserId;
        this.createdByUsername = createdByUsername;
        this.category = category;
        this.priority = priority;
        this.createdAt = createdAt;
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getCreatedByUserId() {
        return createdByUserId;
    }
    
    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
    
    public String getCreatedByUsername() {
        return createdByUsername;
    }
    
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
