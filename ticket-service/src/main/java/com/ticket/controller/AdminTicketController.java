package com.ticket.controller;

import com.ticket.dto.*;
import com.ticket.service.AdminTicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/tickets")
public class AdminTicketController {
    
    @Autowired
    private AdminTicketService adminTicketService;
    
    /**
     * Get all tickets with pagination and filtering
     * GET /admin/tickets?page=0&size=10&status=OPEN&priority=HIGH&category=TECHNICAL_ISSUE&search=login
     */
    @GetMapping
    public ResponseEntity<Page<AdminTicketDTO>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String assignedToUserId,
            @RequestParam(required = false) String createdByUserId,
            @RequestParam(required = false) String search
    ) {
        Page<AdminTicketDTO> tickets = adminTicketService.getAllTickets(
                page, size, status, priority, category, assignedToUserId, createdByUserId, search);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get ticket by ID
     * GET /admin/tickets/{ticketId}
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<AdminTicketDTO> getTicketById(@PathVariable String ticketId) {
        AdminTicketDTO ticket = adminTicketService.getTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Change ticket priority
     * PUT /admin/tickets/{ticketId}/priority
     */
    @PutMapping("/{ticketId}/priority")
    public ResponseEntity<AdminTicketDTO> changePriority(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminChangePriorityRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        AdminTicketDTO ticket = adminTicketService.changePriority(
                ticketId, request, adminId, adminUsername);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Change ticket category
     * PUT /admin/tickets/{ticketId}/category
     */
    @PutMapping("/{ticketId}/category")
    public ResponseEntity<AdminTicketDTO> changeCategory(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminChangeCategoryRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        AdminTicketDTO ticket = adminTicketService.changeCategory(
                ticketId, request, adminId, adminUsername);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Change ticket status (force status change)
     * PUT /admin/tickets/{ticketId}/status
     */
    @PutMapping("/{ticketId}/status")
    public ResponseEntity<AdminTicketDTO> changeStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminChangeStatusRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        AdminTicketDTO ticket = adminTicketService.changeStatus(
                ticketId, request, adminId, adminUsername);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Delete ticket (soft delete - changes status to CLOSED)
     * DELETE /admin/tickets/{ticketId}
     */
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Map<String, String>> deleteTicket(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        String message = adminTicketService.deleteTicket(ticketId, hardDelete, adminId, adminUsername);
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Get ticket statistics
     * GET /admin/tickets/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<TicketStatsDTO> getTicketStats() {
        TicketStatsDTO stats = adminTicketService.getTicketStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get user's all tickets (created by user)
     * GET /admin/tickets/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AdminTicketDTO>> getUserTickets(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AdminTicketDTO> tickets = adminTicketService.getUserTickets(userId, page, size);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get agent's assigned tickets
     * GET /admin/tickets/agent/{agentId}
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<Page<AdminTicketDTO>> getAgentTickets(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AdminTicketDTO> tickets = adminTicketService.getAgentTickets(agentId, page, size);
        return ResponseEntity.ok(tickets);
    }
}
