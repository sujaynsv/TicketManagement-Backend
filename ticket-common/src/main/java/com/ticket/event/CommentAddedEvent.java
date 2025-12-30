package com.ticket.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class CommentAddedEvent implements Serializable {
    
    private String commentId;
    private String ticketId;
    private String ticketNumber;
    private String userId;
    private String username;
    private String commentText;
    private Boolean isInternal;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Constructors
    public CommentAddedEvent() {}
    
    public CommentAddedEvent(String commentId, String ticketId, String ticketNumber,
                            String userId, String username, String commentText,
                            Boolean isInternal, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.userId = userId;
        this.username = username;
        this.commentText = commentText;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }
    
    public void setCommentId(String commentId) {
        this.commentId = commentId;
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
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getCommentText() {
        return commentText;
    }
    
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }
    
    public Boolean getIsInternal() {
        return isInternal;
    }
    
    public void setIsInternal(Boolean isInternal) {
        this.isInternal = isInternal;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
