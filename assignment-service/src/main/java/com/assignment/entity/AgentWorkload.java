package com.assignment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_workload")
public class AgentWorkload {
    
    @Id
    @Column(name = "agent_id", length = 50)
    private String agentId;
    
    @Column(name = "agent_username", nullable = false, length = 100)
    private String agentUsername;
    
    @Column(name = "active_tickets", nullable = false)
    private Integer activeTickets = 0;
    
    @Column(name = "total_assigned_tickets", nullable = false)
    private Integer totalAssignedTickets = 0;
    
    @Column(name = "completed_tickets", nullable = false)
    private Integer completedTickets = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentStatus status = AgentStatus.AVAILABLE;
    
    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public AgentWorkload() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public AgentWorkload(String agentId, String agentUsername) {
        this();
        this.agentId = agentId;
        this.agentUsername = agentUsername;
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
    
    public AgentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AgentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getLastAssignedAt() {
        return lastAssignedAt;
    }
    
    public void setLastAssignedAt(LocalDateTime lastAssignedAt) {
        this.lastAssignedAt = lastAssignedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
