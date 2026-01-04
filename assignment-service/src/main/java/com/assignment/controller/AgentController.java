package com.assignment.controller;

import com.assignment.entity.AgentStatus;
import com.assignment.entity.AgentWorkload;
import com.assignment.entity.Assignment;
import com.assignment.entity.TicketCache;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.assignment.service.AssignmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    
    private AgentWorkloadRepository agentWorkloadRepository;

    private AssignmentService assignmentService;

    private AssignmentRepository assignmentRepository;

    private final TicketCacheRepository ticketCacheRepository;

    public AgentController(AgentWorkloadRepository agentWorkloadRepository, AssignmentService assignmentService, AssignmentRepository assignmentRepository, TicketCacheRepository ticketCacheRepository){
        this.assignmentService=assignmentService;
        this.agentWorkloadRepository=agentWorkloadRepository;
        this.assignmentRepository=assignmentRepository;
        this.ticketCacheRepository=ticketCacheRepository;
    }
    
    /**
     * Getting agents from the auth service
     */

    @PostMapping("/sync")
    public ResponseEntity<String> syncAgents(){
        assignmentService.syncAgentsFromAuthService();
        return ResponseEntity.ok("Agents Successfully fetched from auth service.");
    }

    /**
     * Update agent status (AVAILABLE, BUSY, OFFLINE)
     */
    @PutMapping("/{agentId}/status")
    public ResponseEntity<AgentWorkload> updateAgentStatus(
            @PathVariable String agentId,
            @RequestParam String status) {
        
        AgentWorkload agent = agentWorkloadRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        
        agent.setStatus(AgentStatus.valueOf(status.toUpperCase()));
        agent.setUpdatedAt(LocalDateTime.now());
        
        AgentWorkload updatedAgent = agentWorkloadRepository.save(agent);
        return ResponseEntity.ok(updatedAgent);
    }
    
    /**
     * Get all agents
     */
    @GetMapping
    public ResponseEntity<List<AgentWorkload>> getAllAgents() {
        List<AgentWorkload> agents = agentWorkloadRepository.findAll();
        return ResponseEntity.ok(agents);
    }
    
    /**
     * Get agent by ID
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentWorkload> getAgent(@PathVariable String agentId) {
        AgentWorkload agent = agentWorkloadRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        return ResponseEntity.ok(agent);
    }

    /**
     * Get statistics for the logged-in agent with LIVE data from TicketCache
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAgentStats(
            @RequestHeader("X-User-Id") String agentId) {
        
        // Get all assignments for this agent
        List<Assignment> allAssignments = assignmentRepository.findByAgentId(agentId);
        
        long totalAssigned = allAssignments.size();
        
        // ✅ Count by LIVE ticket status from TicketCache
        long assigned = 0;
        long inProgress = 0;
        long resolved = 0;
        
        for (Assignment assignment : allAssignments) {
            try {
                // Fetch LIVE ticket status from TicketCache
                TicketCache ticket = ticketCacheRepository
                        .findByTicketNumber(assignment.getTicketNumber())
                        .orElse(null);
                
                if (ticket != null) {
                    String status = ticket.getStatus();
                    
                    switch (status) {
                        case "ASSIGNED":
                            assigned++;
                            break;
                        case "IN_PROGRESS":
                            inProgress++;
                            break;
                        case "RESOLVED":
                        case "CLOSED":
                            resolved++;
                            break;
                        default:
                            log.debug("Ticket {} has status: {}", ticket.getTicketNumber(), status);
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching ticket cache: {}", e.getMessage());
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssigned", totalAssigned);
        stats.put("assigned", assigned);
        stats.put("inProgress", inProgress);
        stats.put("resolved", resolved);
        stats.put("avgResolutionTime", 0.0); // TODO: Calculate average
        
        return ResponseEntity.ok(stats);
    }


}
