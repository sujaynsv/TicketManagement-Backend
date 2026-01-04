package com.assignment.controller;

import com.assignment.dto.AgentWorkloadDTO;
import com.assignment.dto.AssignmentDTO;
import com.assignment.dto.ManualAssignmentRequest;
import com.assignment.dto.ReassignmentRequest;
import com.assignment.dto.UnassignedTicketDTO;
import com.assignment.entity.Assignment;
import com.assignment.entity.AssignmentStatus;
import com.assignment.repository.AssignmentRepository;
import com.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    
    private AssignmentService assignmentService;
    
    private AssignmentRepository assignmentRepository;

    public AssignmentController(AssignmentService assignmentService, AssignmentRepository assignmentRepository){
        this.assignmentRepository=assignmentRepository;
        this.assignmentService=assignmentService;
    }

    private static final Logger log = LoggerFactory.getLogger(AssignmentController.class);

    /**
     * Get unassigned tickets for manager dashboard
     */
    @GetMapping("/tickets/unassigned")
    public ResponseEntity<List<UnassignedTicketDTO>> getUnassignedTickets() {
        List<UnassignedTicketDTO> tickets = assignmentService.getUnassignedTickets();
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get available agents with workload
     */
    @GetMapping("/agents/available")
    public ResponseEntity<List<AgentWorkloadDTO>> getAvailableAgents() {
        List<AgentWorkloadDTO> agents = assignmentService.getAvailableAgents();
        return ResponseEntity.ok(agents);
    }
    
    /**
     * Manager manually assigns ticket to agent
     */
    @PostMapping("/manual")
    public ResponseEntity<AssignmentDTO> manualAssignment(
            @Valid @RequestBody ManualAssignmentRequest request,
            @RequestHeader("X-User-Id") String managerId,
            @RequestHeader("X-Username") String managerUsername) {
        
        AssignmentDTO assignment = assignmentService.manualAssignment(
                request, managerId, managerUsername);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
    
    /**
     * Get tickets assigned to an agent
     */
    @GetMapping("/agents/{agentId}/tickets")
    public ResponseEntity<List<AssignmentDTO>> getAgentTickets(
            @PathVariable String agentId) {
        
        List<AssignmentDTO> tickets = assignmentService.getAgentTickets(agentId);
        return ResponseEntity.ok(tickets);
    }
    
    @PutMapping("/reassign")
    public ResponseEntity<String> reassignTickets(
        @Valid @RequestBody ReassignmentRequest request,
        @RequestHeader("X-User-Id") String managerId,
        @RequestHeader("X-Username") String managerUsername
    ){
        try{
            assignmentService.reassignTicket(
                request.getTicketId(),
                request.getNewAgentId(),
                managerId,
                managerUsername
            );
            return ResponseEntity.ok("Ticket Reassigned successfully");
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Reassignment Failed: "+e.getMessage());
        }
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<AssignmentDTO> getAssignmentByTicketId(@PathVariable String ticketId) {
        log.info("Fetching assignment for ticket: {}", ticketId);
        
        // Find ASSIGNED assignment (current active assignment)
        Assignment assignment = assignmentRepository.findByTicketIdAndStatus(ticketId, AssignmentStatus.ASSIGNED)
                .orElseThrow(() -> new RuntimeException("No active assignment found for ticket: " + ticketId));
        
        AssignmentDTO dto = new AssignmentDTO();
        dto.setAssignmentId(assignment.getAssignmentId());
        dto.setTicketId(assignment.getTicketId());
        dto.setTicketNumber(assignment.getTicketNumber());
        dto.setAgentId(assignment.getAgentId());
        dto.setAgentUsername(assignment.getAgentUsername());
        dto.setAssignedBy(assignment.getAssignedBy());
        dto.setAssignedByUsername(assignment.getAssignedByUsername());
        dto.setAssignmentType(assignment.getAssignmentType().name());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setStatus(assignment.getStatus().name());
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my")
    public ResponseEntity<List<AssignmentDTO>> getMyAssignments(
        @RequestHeader("X-User-Id") String agentId,
        @RequestHeader("X-Username") String username
    ){
        log.info("Fetching Agents for agents: {} ({})", username, agentId);

        List<AssignmentDTO> assignments=assignmentService.getAgentAssignments(agentId);

        return ResponseEntity.ok(assignments);
    }
    private AssignmentDTO convertToDTO(Assignment assignment) {
    AssignmentDTO dto = new AssignmentDTO();
    dto.setAssignmentId(assignment.getAssignmentId());
    dto.setTicketId(assignment.getTicketId());
    dto.setTicketNumber(assignment.getTicketNumber());
    dto.setAgentId(assignment.getAgentId());
    dto.setAgentUsername(assignment.getAgentUsername());
    dto.setAssignedBy(assignment.getAssignedBy());
    dto.setAssignedByUsername(assignment.getAssignedByUsername());
    dto.setAssignmentType(assignment.getAssignmentType().name());
    dto.setAssignedAt(assignment.getAssignedAt());
    dto.setStatus(assignment.getStatus().name());

        dto.setTitle(assignment.getTicketTitle());
    dto.setDescription(assignment.getTicketDescription());
    dto.setTicketStatus(assignment.getTicketStatus());
    dto.setTicketPriority(assignment.getTicketPriority());
    dto.setTicketCategory(assignment.getTicketCategory());
    dto.setCreatedByUsername(assignment.getCreatedByUsername());

    return dto;
    }

}
