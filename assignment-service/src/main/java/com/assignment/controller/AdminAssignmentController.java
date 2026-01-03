package com.assignment.controller;

import com.assignment.dto.*;
import com.assignment.service.AdminAssignmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/assignments")
public class AdminAssignmentController {
    
    private AdminAssignmentService adminAssignmentService;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService){
        this.adminAssignmentService=adminAssignmentService;
    }
    
    /**
     * Get all assignments with pagination and filtering
     * GET /admin/assignments?page=0&size=10&status=ASSIGNED&agentId=xxx&ticketId=xxx
     */
    @GetMapping
    public ResponseEntity<Page<AdminAssignmentDTO>> getAllAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String ticketId,
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) String search
    ) {
        Page<AdminAssignmentDTO> assignments = adminAssignmentService.getAllAssignments(
                page, size, status, agentId, ticketId, assignmentType, search);
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get assignment by ID
     * GET /admin/assignments/{assignmentId}
     */
    @GetMapping("/{assignmentId}")
    public ResponseEntity<AdminAssignmentDTO> getAssignmentById(@PathVariable String assignmentId) {
        AdminAssignmentDTO assignment = adminAssignmentService.getAssignmentById(assignmentId);
        return ResponseEntity.ok(assignment);
    }
    
    /**
     * Force reassign ticket to different agent
     * PUT /admin/assignments/{assignmentId}/reassign
     */
    @PutMapping("/{assignmentId}/reassign")
    public ResponseEntity<AdminAssignmentDTO> forceReassign(
            @PathVariable String assignmentId,
            @Valid @RequestBody AdminReassignRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        AdminAssignmentDTO assignment = adminAssignmentService.forceReassign(
                assignmentId, request, adminId, adminUsername);
        return ResponseEntity.ok(assignment);
    }
    
    /**
     * Unassign ticket (remove agent assignment)
     * PUT /admin/assignments/{assignmentId}/unassign
     */
    @PutMapping("/{assignmentId}/unassign")
    public ResponseEntity<Map<String, String>> unassignTicket(
            @PathVariable String assignmentId,
            @Valid @RequestBody UnassignRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        String message = adminAssignmentService.unassignTicket(
                assignmentId, request.reason(), adminId, adminUsername);
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Delete assignment record (hard delete - use with caution)
     * DELETE /admin/assignments/{assignmentId}
     */
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Map<String, String>> deleteAssignment(
            @PathVariable String assignmentId,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        String message = adminAssignmentService.deleteAssignment(assignmentId, adminId, adminUsername);
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Get assignment statistics
     * GET /admin/assignments/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AssignmentStatsDTO> getAssignmentStats() {
        AssignmentStatsDTO stats = adminAssignmentService.getAssignmentStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get agent workload details
     * GET /admin/assignments/agent/{agentId}
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<AgentWorkloadDetailsDTO> getAgentWorkload(@PathVariable String agentId) {
        AgentWorkloadDetailsDTO workload = adminAssignmentService.getAgentWorkload(agentId);
        return ResponseEntity.ok(workload);
    }
    
    /**
     * Get agent's active assignments
     * GET /admin/assignments/agent/{agentId}/active
     */
    @GetMapping("/agent/{agentId}/active")
    public ResponseEntity<List<AdminAssignmentDTO>> getAgentActiveAssignments(
            @PathVariable String agentId) {
        List<AdminAssignmentDTO> assignments = adminAssignmentService.getAgentActiveAssignments(agentId);
        return ResponseEntity.ok(assignments);
    }
    
    /**
     * Get all unassigned tickets
     * GET /admin/assignments/unassigned
     */
    @GetMapping("/unassigned")
    public ResponseEntity<List<UnassignedTicketDTO>> getUnassignedTickets() {
        List<UnassignedTicketDTO> tickets = adminAssignmentService.getUnassignedTickets();
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get assignment history for a ticket
     * GET /admin/assignments/ticket/{ticketId}/history
     */
    @GetMapping("/ticket/{ticketId}/history")
    public ResponseEntity<List<AdminAssignmentDTO>> getTicketAssignmentHistory(
            @PathVariable String ticketId) {
        List<AdminAssignmentDTO> history = adminAssignmentService.getTicketAssignmentHistory(ticketId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Bulk reassign tickets from one agent to another
     * POST /admin/assignments/bulk-reassign
     */
    @PostMapping("/bulk-reassign")
    public ResponseEntity<Map<String, Object>> bulkReassign(
            @Valid @RequestBody BulkReassignRequest request,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-Username") String adminUsername
    ) {
        Map<String, Object> result = adminAssignmentService.bulkReassign(
                request, adminId, adminUsername);
        return ResponseEntity.ok(result);
    }
}
