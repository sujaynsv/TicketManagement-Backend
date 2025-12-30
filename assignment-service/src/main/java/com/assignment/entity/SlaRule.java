package com.assignment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_rules")
public class SlaRule {
    
    @Id
    @Column(name = "rule_id", length = 36)
    private String ruleId;
    
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "response_time_minutes", nullable = false)
    private Integer responseTimeMinutes;
    
    @Column(name = "resolution_time_hours", nullable = false)
    private Integer resolutionTimeHours;
    
    @Column(name = "business_hours_only", nullable = false)
    private Boolean businessHoursOnly = true;
    
    @Column(name = "escalation_time_minutes")
    private Integer escalationTimeMinutes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public SlaRule() {
        this.ruleId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public SlaRule(String priority, String category, Integer responseTimeMinutes, 
                   Integer resolutionTimeHours) {
        this();
        this.priority = priority;
        this.category = category;
        this.responseTimeMinutes = responseTimeMinutes;
        this.resolutionTimeHours = resolutionTimeHours;
    }
    
    // Getters and Setters
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
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
    
    public Integer getResponseTimeMinutes() {
        return responseTimeMinutes;
    }
    
    public void setResponseTimeMinutes(Integer responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }
    
    public Integer getResolutionTimeHours() {
        return resolutionTimeHours;
    }
    
    public void setResolutionTimeHours(Integer resolutionTimeHours) {
        this.resolutionTimeHours = resolutionTimeHours;
    }
    
    public Boolean getBusinessHoursOnly() {
        return businessHoursOnly;
    }
    
    public void setBusinessHoursOnly(Boolean businessHoursOnly) {
        this.businessHoursOnly = businessHoursOnly;
    }
    
    public Integer getEscalationTimeMinutes() {
        return escalationTimeMinutes;
    }
    
    public void setEscalationTimeMinutes(Integer escalationTimeMinutes) {
        this.escalationTimeMinutes = escalationTimeMinutes;
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
