package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SlaBreachEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String trackingId;
    private String ticketId;
    private String ticketNumber;
    private String priority;
    private String category;
    private String breachType;
    private LocalDateTime dueAt;
    private LocalDateTime breachedAt;
    private Integer minutesOverdue;
    private String breachReason;
    private String assignedAgentId;
    private String assignedAgentUsername;
    private String assignedAgentEmail;
    private Boolean responseBreached;
    private Boolean resolutionBreached;
    
    public SlaBreachEvent() {
        this.breachedAt = LocalDateTime.now();
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
    
    public String getBreachType() {
        return breachType;
    }
    
    public void setBreachType(String breachType) {
        this.breachType = breachType;
    }
    
    public LocalDateTime getDueAt() {
        return dueAt;
    }
    
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
    
    public LocalDateTime getBreachedAt() {
        return breachedAt;
    }
    
    public void setBreachedAt(LocalDateTime breachedAt) {
        this.breachedAt = breachedAt;
    }
    
    public Integer getMinutesOverdue() {
        return minutesOverdue;
    }
    
    public void setMinutesOverdue(Integer minutesOverdue) {
        this.minutesOverdue = minutesOverdue;
    }
    
    public String getBreachReason() {
        return breachReason;
    }
    
    public void setBreachReason(String breachReason) {
        this.breachReason = breachReason;
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
    
    public Boolean getResponseBreached() {
        return responseBreached;
    }
    
    public void setResponseBreached(Boolean responseBreached) {
        this.responseBreached = responseBreached;
    }
    
    public Boolean getResolutionBreached() {
        return resolutionBreached;
    }
    
    public void setResolutionBreached(Boolean resolutionBreached) {
        this.resolutionBreached = resolutionBreached;
    }
}
