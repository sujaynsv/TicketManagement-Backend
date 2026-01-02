package com.assignment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_tracking")
public class SlaTracking {
    
    @Id
    @Column(name = "tracking_id", length = 36)
    private String trackingId;
    
    @Column(name = "ticket_id", nullable = false, unique = true, length = 50)
    private String ticketId;
    
    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;
    
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;
    
    @Column(name = "category", length = 50)
    private String category;
    
    // SLA timestamps
    @Column(name = "sla_start_time", nullable = false)
    private LocalDateTime slaStartTime;
    
    @Column(name = "response_due_at", nullable = false)
    private LocalDateTime responseDueAt;
    
    @Column(name = "resolution_due_at", nullable = false)
    private LocalDateTime resolutionDueAt;
    
    // First response tracking
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;
    
    @Column(name = "response_breached", nullable = false)
    private Boolean responseBreached = false;
    
    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;
    
    // Resolution tracking
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolution_breached", nullable = false)
    private Boolean resolutionBreached = false;
    
    @Column(name = "resolution_time_hours", precision = 10, scale = 2)
    private BigDecimal resolutionTimeHours;
    
    // SLA status
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", nullable = false, length = 20)
    private SlaStatus slaStatus = SlaStatus.ON_TIME;
    
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;
    
    @Column(name = "paused_duration_minutes", nullable = false)
    private Integer pausedDurationMinutes = 0;
    
    // Breach details
    @Column(name = "breach_reason", length = 200)
    private String breachReason;
    
    @Column(name = "breached_at")
    private LocalDateTime breachedAt;
    
    // Metadata
    @Column(name = "assigned_agent_id", length = 50)
    private String assignedAgentId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public SlaTracking() {
        this.trackingId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
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
    
    public LocalDateTime getSlaStartTime() {
        return slaStartTime;
    }
    
    public void setSlaStartTime(LocalDateTime slaStartTime) {
        this.slaStartTime = slaStartTime;
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
    
    public SlaStatus getSlaStatus() {
        return slaStatus;
    }
    
    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }
    
    public LocalDateTime getPausedAt() {
        return pausedAt;
    }
    
    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }
    
    public Integer getPausedDurationMinutes() {
        return pausedDurationMinutes;
    }
    
    public void setPausedDurationMinutes(Integer pausedDurationMinutes) {
        this.pausedDurationMinutes = pausedDurationMinutes;
    }
    
    public String getBreachReason() {
        return breachReason;
    }
    
    public void setBreachReason(String breachReason) {
        this.breachReason = breachReason;
    }
    
    public LocalDateTime getBreachedAt() {
        return breachedAt;
    }
    
    public void setBreachedAt(LocalDateTime breachedAt) {
        this.breachedAt = breachedAt;
    }
    
    public String getAssignedAgentId() {
        return assignedAgentId;
    }
    
    public void setAssignedAgentId(String assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
