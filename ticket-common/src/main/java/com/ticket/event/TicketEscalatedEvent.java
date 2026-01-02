package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TicketEscalatedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String ticketId;
    private String ticketNumber;
    private String title;
    private String category;
    private String priority;
    private String escalationType;
    private String escalationReason;
    private String escalatedBy;
    private String escalatedByUsername;
    private String escalatedToUserId;
    private String escalatedToUsername;
    private String escalatedToEmail;
    private String previousAgentId;
    private String previousAgentUsername;
    private LocalDateTime escalatedAt;
    
    public TicketEscalatedEvent() {
        this.escalatedAt = LocalDateTime.now();
    }
    
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
    
    public String getEscalationType() {
        return escalationType;
    }
    
    public void setEscalationType(String escalationType) {
        this.escalationType = escalationType;
    }
    
    public String getEscalationReason() {
        return escalationReason;
    }
    
    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }
    
    public String getEscalatedBy() {
        return escalatedBy;
    }
    
    public void setEscalatedBy(String escalatedBy) {
        this.escalatedBy = escalatedBy;
    }
    
    public String getEscalatedByUsername() {
        return escalatedByUsername;
    }
    
    public void setEscalatedByUsername(String escalatedByUsername) {
        this.escalatedByUsername = escalatedByUsername;
    }
    
    public String getEscalatedToUserId() {
        return escalatedToUserId;
    }
    
    public void setEscalatedToUserId(String escalatedToUserId) {
        this.escalatedToUserId = escalatedToUserId;
    }
    
    public String getEscalatedToUsername() {
        return escalatedToUsername;
    }
    
    public void setEscalatedToUsername(String escalatedToUsername) {
        this.escalatedToUsername = escalatedToUsername;
    }
    
    public String getEscalatedToEmail() {
        return escalatedToEmail;
    }
    
    public void setEscalatedToEmail(String escalatedToEmail) {
        this.escalatedToEmail = escalatedToEmail;
    }
    
    public String getPreviousAgentId() {
        return previousAgentId;
    }
    
    public void setPreviousAgentId(String previousAgentId) {
        this.previousAgentId = previousAgentId;
    }
    
    public String getPreviousAgentUsername() {
        return previousAgentUsername;
    }
    
    public void setPreviousAgentUsername(String previousAgentUsername) {
        this.previousAgentUsername = previousAgentUsername;
    }
    
    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }
    
    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }
}
