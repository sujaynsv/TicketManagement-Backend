package com.ticket.controller;

import com.ticket.dto.*;
import com.ticket.service.AttachmentService;
import com.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
// import org.springframework.web.multipart.MultipartFile;
// import java.io.IOException;


@RestController
@RequestMapping("/tickets")
public class TicketController {
    
    @Autowired
    private TicketService ticketService;

    @Autowired
    private AttachmentService attachmentService; 
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public String health() {
        return "Ticket Service is running!";
    }
    
    /**
     * Create new ticket, including optional attachements
     */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDTO> createTicket(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("priority") String priority,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) throws IOException {
        
        // Build request object
        CreateTicketRequest request = new CreateTicketRequest(
            title, description, category, priority, tags
        );
        
        // Step 1: Create ticket (without attachments)
        TicketDTO ticket = ticketService.createTicket(request, userId, username);
        
        // Step 2: Upload attachments if provided
        if (files != null && !files.isEmpty()) {
            int uploadedCount = 0;
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    attachmentService.uploadAttachment(ticket.ticketId(), file, userId, username);
                    uploadedCount++;
                }
            }
            
            // Step 3: Update attachment count
            if (uploadedCount > 0) {
                ticketService.updateAttachmentCount(ticket.ticketId(), uploadedCount);
                
                ticket = new TicketDTO(
                    ticket.ticketId(),
                    ticket.ticketNumber(),
                    ticket.title(),
                    ticket.description(),
                    ticket.status(),
                    ticket.category(),
                    ticket.priority(),
                    ticket.createdByUserId(),
                    ticket.createdByUsername(),
                    ticket.assignedToUserId(),
                    ticket.assignedToUsername(),
                    ticket.tags(),
                    ticket.commentCount(),
                    uploadedCount,  
                    ticket.createdAt(),
                    ticket.updatedAt(),
                    ticket.assignedAt(),
                    ticket.resolvedAt(),
                    ticket.closedAt()
                );
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }



    
    /**
     * Get ticket by ID
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable String ticketId) {
        TicketDTO ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Get ticket by ticket number
     */
    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketDTO> getTicketByNumber(@PathVariable String ticketNumber) {
        TicketDTO ticket = ticketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Update ticket
     */
    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketDTO> updateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        TicketDTO ticket = ticketService.updateTicket(ticketId, request, userId, username);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Change ticket status
     */
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketDTO> changeStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody ChangeStatusRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        TicketDTO ticket = ticketService.changeStatus(ticketId, request, userId, username);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Get my tickets (created by me)
     */
    @GetMapping("/my")
    public ResponseEntity<List<TicketDTO>> getMyTickets(
            @RequestHeader("X-User-Id") String userId) {
        List<TicketDTO> tickets = ticketService.getMyTickets(userId);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get tickets assigned to me
     */
    @GetMapping("/assigned")
    public ResponseEntity<List<TicketDTO>> getAssignedTickets(
            @RequestHeader("X-User-Id") String userId) {
        List<TicketDTO> tickets = ticketService.getAssignedTickets(userId);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get tickets by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketDTO>> getTicketsByStatus(@PathVariable String status) {
        List<TicketDTO> tickets = ticketService.getTicketsByStatus(status);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get all tickets
     */
    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets() {
        List<TicketDTO> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Delete ticket (admin only)
     */
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<String> deleteTicket(@PathVariable String ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok("Ticket deleted successfully");
    }
}
