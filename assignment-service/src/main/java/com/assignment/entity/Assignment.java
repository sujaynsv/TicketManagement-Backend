package com.assignment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
public class Assignment {
    
    @Id
    @Column(name = "assignment_id", length = 36)
    private String assignmentId;
    
    @Column(name = "ticket_id", nullable = false, length = 50)
    private String ticketId;
    
    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;
    
    @Column(name = "agent_id", nullable = false, length = 50)
    private String agentId;
    
    @Column(name = "agent_username", nullable = false, length = 100)
    private String agentUsername;
    
    @Column(name = "assigned_by", nullable = false, length = 50)
    private String assignedBy;
    
    @Column(name = "assigned_by_username", nullable = false, length = 100)
    private String assignedByUsername;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private AssignmentType assignmentType;
    
    @Column(name = "assignment_strategy", length = 50)
    private String assignmentStrategy;
    
    @Column(name = "previous_agent_id", length = 50)
    private String previousAgentId;
    
    @Column(name = "previous_agent_username", length = 100)
    private String previousAgentUsername;
    
    @Column(name = "reassignment_reason", length = 500)
    private String reassignmentReason;
    
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status;
    
    // Constructors
    public Assignment() {
        this.assignmentId = UUID.randomUUID().toString();
        this.assignedAt = LocalDateTime.now();
        this.status = AssignmentStatus.ACTIVE;
    }
    
    public Assignment(String ticketId, String ticketNumber, String agentId, 
                     String agentUsername, String assignedBy, String assignedByUsername,
                     AssignmentType assignmentType) {
        this();
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.agentId = agentId;
        this.agentUsername = agentUsername;
        this.assignedBy = assignedBy;
        this.assignedByUsername = assignedByUsername;
        this.assignmentType = assignmentType;
    }
    
    // Getters and Setters
    public String getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
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
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public String getAssignedByUsername() {
        return assignedByUsername;
    }
    
    public void setAssignedByUsername(String assignedByUsername) {
        this.assignedByUsername = assignedByUsername;
    }
    
    public AssignmentType getAssignmentType() {
        return assignmentType;
    }
    
    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }
    
    public String getAssignmentStrategy() {
        return assignmentStrategy;
    }
    
    public void setAssignmentStrategy(String assignmentStrategy) {
        this.assignmentStrategy = assignmentStrategy;
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
    
    public String getReassignmentReason() {
        return reassignmentReason;
    }
    
    public void setReassignmentReason(String reassignmentReason) {
        this.reassignmentReason = reassignmentReason;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public AssignmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
