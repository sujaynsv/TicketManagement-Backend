package com.ticket.controller;

import com.ticket.dto.AttachmentDTO;
import com.ticket.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Upload attachment
     * POST /api/tickets/{ticketId}/attachments/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<AttachmentDTO> uploadAttachment(
            @PathVariable String ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) throws IOException {
        
        log.info(" Upload - Ticket: {}, File: {}", ticketId, file.getOriginalFilename());
        
        try {
            AttachmentDTO attachment = attachmentService.uploadAttachment(ticketId, file, userId, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
        } catch (IOException e) {
            log.error(" Upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Get all attachments for ticket
     * GET /api/tickets/{ticketId}/attachments
     */
    @GetMapping
    public ResponseEntity<List<AttachmentDTO>> getAttachments(@PathVariable String ticketId) {
        log.info("  Get attachments - Ticket: {}", ticketId);
        
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsByTicket(ticketId);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Download single attachment
     * GET /api/tickets/{ticketId}/attachments/{attachmentId}/download
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable String ticketId,
            @PathVariable String attachmentId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("  Download - Ticket: {}, Attachment: {}", ticketId, attachmentId);
        
        // Verify user has access to ticket
        if (!attachmentService.canUserAccessTicket(ticketId, userId)) {
            log.warn(" Access denied - User: {}, Ticket: {}", userId, ticketId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return attachmentService.downloadAttachment(ticketId, attachmentId, userId);
    }

    /**
     * Download all attachments as ZIP
     * GET /api/tickets/{ticketId}/attachments/download/all
     */
    @GetMapping("/download/all")
    public ResponseEntity<byte[]> downloadAllAttachments(
            @PathVariable String ticketId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("📦 Download all - Ticket: {}", ticketId);
        
        // Verify user has access to ticket
        if (!attachmentService.canUserAccessTicket(ticketId, userId)) {
            log.warn(" Access denied - User: {}, Ticket: {}", userId, ticketId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return attachmentService.downloadAllAttachments(ticketId, userId);
    }

    /**
     * Delete attachment
     * DELETE /api/tickets/{ticketId}/attachments/{attachmentId}
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable String ticketId,
            @PathVariable String attachmentId) {
        
        log.info("🗑️  Delete - Ticket: {}, Attachment: {}", ticketId, attachmentId);
        
        try {
            attachmentService.deleteAttachment(attachmentId, ticketId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(" Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
