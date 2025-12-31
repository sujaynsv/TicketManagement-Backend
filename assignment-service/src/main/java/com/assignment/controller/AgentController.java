package com.assignment.controller;

import com.assignment.entity.AgentStatus;
import com.assignment.entity.AgentWorkload;
import com.assignment.repository.AgentWorkloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/agents")
public class AgentController {
    
    @Autowired
    private AgentWorkloadRepository agentWorkloadRepository;
    
    /**
     * Register a new agent (or get if exists)
     */
    @PostMapping("/register")
    public ResponseEntity<AgentWorkload> registerAgent(
            @RequestParam String agentId,
            @RequestParam String agentUsername) {
        
        // Check if agent already exists
        AgentWorkload agent = agentWorkloadRepository.findById(agentId)
                .orElseGet(() -> {
                    AgentWorkload newAgent = new AgentWorkload(agentId, agentUsername);
                    return agentWorkloadRepository.save(newAgent);
                });
        
        return ResponseEntity.status(HttpStatus.CREATED).body(agent);
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
