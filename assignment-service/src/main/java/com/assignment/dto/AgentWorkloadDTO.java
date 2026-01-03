package com.assignment.dto;

import java.time.LocalDateTime;

public class AgentWorkloadDTO {
    
    private String agentId;
    private String agentUsername;
    private Integer activeTickets;
    private Integer totalAssignedTickets;
    private Integer completedTickets;
    private String status;
    private LocalDateTime lastAssignedAt;
    private Boolean isRecommended;
    
    // Constructors
    // Default constructor required for frameworks like Jackson or JPA
    public AgentWorkloadDTO() {
        //
    }
    
    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getAgentUsername() {
        return agentUsername;
    }
    
    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }
    
    public Integer getActiveTickets() {
        return activeTickets;
    }
    
    public void setActiveTickets(Integer activeTickets) {
        this.activeTickets = activeTickets;
    }
    
    public Integer getTotalAssignedTickets() {
        return totalAssignedTickets;
    }
    
    public void setTotalAssignedTickets(Integer totalAssignedTickets) {
        this.totalAssignedTickets = totalAssignedTickets;
    }
    
    public Integer getCompletedTickets() {
        return completedTickets;
    }
    
    public void setCompletedTickets(Integer completedTickets) {
        this.completedTickets = completedTickets;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastAssignedAt() {
        return lastAssignedAt;
    }
    
    public void setLastAssignedAt(LocalDateTime lastAssignedAt) {
        this.lastAssignedAt = lastAssignedAt;
    }
    
    public Boolean getIsRecommended() {
        return isRecommended;
    }
    
    public void setIsRecommended(Boolean isRecommended) {
        this.isRecommended = isRecommended;
    }
}
