package com.assignment.dto;

import jakarta.validation.constraints.NotBlank;

public class ManualAssignmentRequest {
    
    @NotBlank(message = "Ticket ID is required")
    private String ticketId;
    
    @NotBlank(message = "Agent ID is required")
    private String agentId;

    @NotBlank(message = "Priority is required")
    private String priority; 
    
    private String assignmentNote;
    
    // Constructors
    public ManualAssignmentRequest() {}
    
    public ManualAssignmentRequest(String ticketId, String agentId, String assignmentNote, String priority) {
        this.ticketId = ticketId;
        this.agentId = agentId;
        this.assignmentNote = assignmentNote;
        this.priority=priority;
    }
    
    public void setPriority(String priority){
        this.priority=priority;
    }
    public String getPriority(){
        return priority;
    }
    // Getters and Setters
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getAssignmentNote() {
        return assignmentNote;
    }
    
    public void setAssignmentNote(String assignmentNote) {
        this.assignmentNote = assignmentNote;
    }
}
