package com.assignment.dto;

import java.time.LocalDateTime;

@SuppressWarnings("java:S1192")
public class AssignmentDTO {
    
    private String assignmentId;
    private String ticketId;
    private String ticketNumber;
    private String agentId;
    private String agentUsername;
    private String assignedBy;
    private String assignedByUsername;
    private String assignmentType;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    private String title;
    private String description;
    private String ticketStatus;
    private String ticketPriority;
    private String ticketCategory;
    private String createdByUsername;

    public String getTitle(){
        return title;
    }
    public void setTitle(String title){
        this.title=title;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description=description;
    }
    public String getTicketStatus(){
        return ticketStatus;
    }
    public void setTicketStatus(String ticketStatus){
        this.ticketStatus=ticketStatus;
    }
    public String getTicketPriority(){
        return ticketPriority;
    }
    public void setTicketPriority(String ticketPriority){
        this.ticketPriority=ticketPriority;
    }
    public String getTicketCategory(){
        return ticketCategory;
    }
    public void setTicketCategory(String ticketCategory){
        this.ticketCategory=ticketCategory;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }



    
    // Constructors
    public AssignmentDTO() {
        //
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
    
    public String getAssignmentType() {
        return assignmentType;
    }
    
    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
}
