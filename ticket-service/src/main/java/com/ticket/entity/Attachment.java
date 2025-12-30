package com.ticket.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "attachments")
public class Attachment {
    
    @Id
    private String attachmentId;
    
    private String ticketId;
    
    private String fileName;
    
    private String originalFileName;
    
    private String fileType; // MIME type: image/png, application/pdf
    
    private Long fileSize; // in bytes
    
    private String s3Key; // S3 object key
    
    private String s3Url; // Pre-signed URL (temporary)
    
    private String uploadedByUserId;
    
    private String uploadedByUsername;
    
    private LocalDateTime uploadedAt;
    
    // Constructors
    public Attachment() {}
    
    // Getters and Setters
    public String getAttachmentId() {
        return attachmentId;
    }
    
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    public String getS3Url() {
        return s3Url;
    }
    
    public void setS3Url(String s3Url) {
        this.s3Url = s3Url;
    }
    
    public String getUploadedByUserId() {
        return uploadedByUserId;
    }
    
    public void setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }
    
    public String getUploadedByUsername() {
        return uploadedByUsername;
    }
    
    public void setUploadedByUsername(String uploadedByUsername) {
        this.uploadedByUsername = uploadedByUsername;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
