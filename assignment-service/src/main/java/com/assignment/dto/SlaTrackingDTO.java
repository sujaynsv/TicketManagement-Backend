package com.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SlaTrackingDTO {
    
    private String trackingId;
    private String ticketId;
    private String ticketNumber;
    private String priority;
    private String category;
    private LocalDateTime responseDueAt;
    private LocalDateTime resolutionDueAt;
    private LocalDateTime firstResponseAt;
    private Boolean responseBreached;
    private Integer responseTimeMinutes;
    private LocalDateTime resolvedAt;
    private Boolean resolutionBreached;
    private BigDecimal resolutionTimeHours;
    private String slaStatus;
    private String timeRemaining;
    
    // Constructors
    public SlaTrackingDTO() {}
    
    // Getters and Setters
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
    
    public LocalDateTime getResponseDueAt() {
        return responseDueAt;
    }
    
    public void setResponseDueAt(LocalDateTime responseDueAt) {
        this.responseDueAt = responseDueAt;
    }
    
    public LocalDateTime getResolutionDueAt() {
        return resolutionDueAt;
    }
    
    public void setResolutionDueAt(LocalDateTime resolutionDueAt) {
        this.resolutionDueAt = resolutionDueAt;
    }
    
    public LocalDateTime getFirstResponseAt() {
        return firstResponseAt;
    }
    
    public void setFirstResponseAt(LocalDateTime firstResponseAt) {
        this.firstResponseAt = firstResponseAt;
    }
    
    public Boolean getResponseBreached() {
        return responseBreached;
    }
    
    public void setResponseBreached(Boolean responseBreached) {
        this.responseBreached = responseBreached;
    }
    
    public Integer getResponseTimeMinutes() {
        return responseTimeMinutes;
    }
    
    public void setResponseTimeMinutes(Integer responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }
    
    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public Boolean getResolutionBreached() {
        return resolutionBreached;
    }
    
    public void setResolutionBreached(Boolean resolutionBreached) {
        this.resolutionBreached = resolutionBreached;
    }
    
    public BigDecimal getResolutionTimeHours() {
        return resolutionTimeHours;
    }
    
    public void setResolutionTimeHours(BigDecimal resolutionTimeHours) {
        this.resolutionTimeHours = resolutionTimeHours;
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
