package com.assignment.controller;

import com.assignment.entity.AgentStatus;
import com.assignment.entity.AgentWorkload;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.service.AssignmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {
    
    private AgentWorkloadRepository agentWorkloadRepository;

    private AssignmentService assignmentService;

    public AgentController(AgentWorkloadRepository agentWorkloadRepository, AssignmentService assignmentService){
        this.assignmentService=assignmentService;
        this.agentWorkloadRepository=agentWorkloadRepository;
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
}
