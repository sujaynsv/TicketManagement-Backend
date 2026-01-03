package com.ticket.controller;

import com.ticket.dto.AttachmentDTO;
import com.ticket.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
public class AttachmentController {
    
    private AttachmentService attachmentService;

    public AttachmentController( AttachmentService attachmentService){
        this.attachmentService=attachmentService;
    }
    
    /**
     * Upload attachment
     */
    @PostMapping
    public ResponseEntity<AttachmentDTO> uploadAttachment(
            @PathVariable String ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) throws IOException {
        AttachmentDTO attachment = attachmentService.uploadAttachment(ticketId, file, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }
    
    /**
     * Get all attachments for ticket
     */
    @GetMapping
    public ResponseEntity<List<AttachmentDTO>> getAttachments(@PathVariable String ticketId) {
        List<AttachmentDTO> attachments = attachmentService.getAttachmentsByTicket(ticketId);
        return ResponseEntity.ok(attachments);
    }
    
    /**
     * Delete attachment
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<String> deleteAttachment(@PathVariable String attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.ok("Attachment deleted successfully");
    }
}
