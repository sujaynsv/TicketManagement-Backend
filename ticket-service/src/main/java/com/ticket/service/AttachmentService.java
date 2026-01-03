package com.ticket.service;

import com.ticket.dto.AttachmentDTO;
import com.ticket.entity.Attachment;
import com.ticket.repository.AttachmentRepository;
import com.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttachmentService {
    
    private AttachmentRepository attachmentRepository;
    
    private TicketRepository ticketRepository;
    
    private S3StorageService s3StorageService;
    
    private TicketService ticketService;

    public AttachmentService( AttachmentRepository attachmentRepository, TicketRepository ticketRepository, S3StorageService s3StorageService, TicketService ticketService){
        this.attachmentRepository=attachmentRepository;
        this.ticketRepository=ticketRepository;
        this.s3StorageService=s3StorageService;
        this.ticketService=ticketService;
    }
    
    /**
     * Upload attachment
     */
    @Transactional
    public AttachmentDTO uploadAttachment(String ticketId, MultipartFile file, 
                                         String userId, String username) throws IOException {
        // Verify ticket exists
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Upload to S3
        String s3Key = s3StorageService.uploadFile(ticketId, file);
        
        // Create attachment record
        Attachment attachment = new Attachment();
        attachment.setTicketId(ticketId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setOriginalFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setS3Key(s3Key);
        attachment.setUploadedByUserId(userId);
        attachment.setUploadedByUsername(username);
        attachment.setUploadedAt(LocalDateTime.now());
        
        Attachment savedAttachment = attachmentRepository.save(attachment);
        
        // Increment ticket attachment count
        ticketService.incrementAttachmentCount(ticketId);
        
        // Generate pre-signed URL
        String presignedUrl = s3StorageService.generatePresignedUrl(s3Key);
        savedAttachment.setS3Url(presignedUrl);
        
        return convertToDTO(savedAttachment);
    }
    
    /**
     * Get attachments for ticket
     */
    public List<AttachmentDTO> getAttachmentsByTicket(String ticketId) {
        List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId);
        
        // Generate fresh pre-signed URLs
        return attachments.stream().map(attachment -> {
            String presignedUrl = s3StorageService.generatePresignedUrl(attachment.getS3Key());
            attachment.setS3Url(presignedUrl);
            return convertToDTO(attachment);
        }).toList();
    }
    
    /**
     * Delete attachment
     */
    @Transactional
    public void deleteAttachment(String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        
        // Delete from S3
        s3StorageService.deleteFile(attachment.getS3Key());
        
        // Delete from database
        attachmentRepository.deleteById(attachmentId);
    }
    
    /**
     * Convert entity to DTO
     */
    private AttachmentDTO convertToDTO(Attachment attachment) {
        return new AttachmentDTO(
                attachment.getAttachmentId(),
                attachment.getTicketId(),
                attachment.getFileName(),
                attachment.getOriginalFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getS3Url(),
                attachment.getUploadedByUserId(),
                attachment.getUploadedByUsername(),
                attachment.getUploadedAt()
        );
    }
}
