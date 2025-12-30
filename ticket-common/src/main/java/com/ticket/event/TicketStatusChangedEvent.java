package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TicketStatusChangedEvent implements Serializable {
    
    private String ticketId;
    private String ticketNumber;
    private String oldStatus;
    private String newStatus;
    private String changedByUserId;
    private String changedByUsername;
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
    
    // Constructors
    public TicketStatusChangedEvent() {}
    
    public TicketStatusChangedEvent(String ticketId, String ticketNumber,
                                   String oldStatus, String newStatus,
                                   String changedByUserId, String changedByUsername,
                                   String comment, LocalDateTime changedAt) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedByUserId = changedByUserId;
        this.changedByUsername = changedByUsername;
        this.comment = comment;
        this.changedAt = changedAt;
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
    
    public String getOldStatus() {
        return oldStatus;
    }
    
    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getChangedByUserId() {
        return changedByUserId;
    }
    
    public void setChangedByUserId(String changedByUserId) {
        this.changedByUserId = changedByUserId;
    }
    
    public String getChangedByUsername() {
        return changedByUsername;
    }
    
    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
