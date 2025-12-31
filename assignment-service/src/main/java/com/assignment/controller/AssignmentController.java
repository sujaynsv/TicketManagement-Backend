package com.assignment.controller;

import com.assignment.dto.AgentWorkloadDTO;
import com.assignment.dto.AssignmentDTO;
import com.assignment.dto.ManualAssignmentRequest;
import com.assignment.dto.UnassignedTicketDTO;
import com.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {
    
    @Autowired
    private AssignmentService assignmentService;
    
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
}
