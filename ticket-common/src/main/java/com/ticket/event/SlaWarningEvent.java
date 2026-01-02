package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SlaWarningEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String trackingId;
    private String ticketId;
    private String ticketNumber;
    private String priority;
    private String category;
    private String warningType;
    private LocalDateTime dueAt;
    private Integer minutesRemaining;
    private Double percentageTimeUsed;
    private String assignedAgentId;
    private String assignedAgentUsername;
    private String assignedAgentEmail;
    private LocalDateTime warningTime;
    
    public SlaWarningEvent() {
        this.warningTime = LocalDateTime.now();
    }
    
    public String getTrackingId() {
        return trackingId;
    }
    
    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
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
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getWarningType() {
        return warningType;
    }
    
    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }
    
    public LocalDateTime getDueAt() {
        return dueAt;
    }
    
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
    
    public Integer getMinutesRemaining() {
        return minutesRemaining;
    }
    
    public void setMinutesRemaining(Integer minutesRemaining) {
        this.minutesRemaining = minutesRemaining;
    }
    
    public Double getPercentageTimeUsed() {
        return percentageTimeUsed;
    }
    
    public void setPercentageTimeUsed(Double percentageTimeUsed) {
        this.percentageTimeUsed = percentageTimeUsed;
    }
    
    public String getAssignedAgentId() {
        return assignedAgentId;
    }
    
    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }
    
    public String getAssignedAgentUsername() {
        return assignedAgentUsername;
    }
    
    public void setAssignedAgentUsername(String assignedAgentUsername) {
        this.assignedAgentUsername = assignedAgentUsername;
    }
    
    public String getAssignedAgentEmail() {
        return assignedAgentEmail;
    }
    
    public void setAssignedAgentEmail(String assignedAgentEmail) {
        this.assignedAgentEmail = assignedAgentEmail;
    }
    
    public LocalDateTime getWarningTime() {
        return warningTime;
    }
    
    public void setWarningTime(LocalDateTime warningTime) {
        this.warningTime = warningTime;
    }
}
