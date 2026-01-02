package com.assignment.service;

import com.assignment.client.AuthServiceClient;
import com.assignment.dto.AgentWorkloadDTO;
import com.assignment.dto.AssignmentDTO;
import com.assignment.dto.ManualAssignmentRequest;
import com.assignment.dto.UnassignedTicketDTO;
import com.assignment.entity.*;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.ticket.event.TicketAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssignmentService {
    
    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);
    
    @Autowired
    private TicketCacheRepository ticketCacheRepository;

    @Autowired
    private AuthServiceClient authServiceClient;

    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private AgentWorkloadRepository agentWorkloadRepository;
    
    @Autowired
    private SlaService slaService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Value("${assignment.auto-assign.enabled}")
    private Boolean autoAssignEnabled;
    
    @Value("${assignment.auto-assign.strategy}")
    private String assignmentStrategy;
    
    @Value("${assignment.max-tickets-per-agent}")
    private Integer maxTicketsPerAgent;
    
    /**
     * Get unassigned tickets for manager dashboard
     */
    public List<UnassignedTicketDTO> getUnassignedTickets() {
        List<TicketCache> tickets = ticketCacheRepository.findByStatusAndAssignedAgentIdIsNull("OPEN");
        
        return tickets.stream()
                .map(this::convertToUnassignedTicketDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get available agents with workload (for manager dashboard)
     */
    public List<AgentWorkloadDTO> getAvailableAgents() {
        List<AgentWorkload> agents = agentWorkloadRepository
                .findByStatusOrderByActiveTicketsAsc(AgentStatus.AVAILABLE);
        
        List<AgentWorkloadDTO> agentDTOs = agents.stream()
                .map(this::convertToAgentWorkloadDTO)
                .collect(Collectors.toList());
        
        // Mark recommended agent (least loaded)
        if (!agentDTOs.isEmpty()) {
            agentDTOs.get(0).setIsRecommended(true);
        }
        
        return agentDTOs;
    }
    
    /**
     * Manager manually assigns ticket to agent
     */
    @Transactional
    public AssignmentDTO manualAssignment(ManualAssignmentRequest request, 
                                        String managerId, String managerUsername) {
        log.info("Processing manual assignment for ticket {} to agent {} with priority {} by manager {}", 
                request.getTicketId(), request.getAgentId(), request.getPriority(), managerUsername);
        
        // Validate ticket exists
        TicketCache ticket = ticketCacheRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Check if already assigned
        if (ticket.getAssignedAgentId() != null) {
            throw new RuntimeException("Ticket already assigned to " + ticket.getAssignedAgentUsername());
        }
        
        //   NEW: Update priority (manager sets it during assignment)
        String oldPriority = ticket.getPriority();
        ticket.setPriority(request.getPriority());
        
        if (oldPriority == null) {
            log.info("Manager set priority to {} for ticket {} during assignment", 
                    request.getPriority(), ticket.getTicketNumber());
        } else {
            log.info("Manager changed priority from {} to {} for ticket {}", 
                    oldPriority, request.getPriority(), ticket.getTicketNumber());
        }
        
        // Validate agent exists
        AgentWorkload agent = agentWorkloadRepository.findById(request.getAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        
        if (agent.getStatus() == AgentStatus.OFFLINE) {
            throw new RuntimeException("Agent is currently offline");
        }
        
        if (agent.getActiveTickets() >= maxTicketsPerAgent) {
            throw new RuntimeException("Agent has reached maximum ticket capacity");
        }
        
        // Create assignment record
        Assignment assignment = new Assignment(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                agent.getAgentId(),
                agent.getAgentUsername(),
                managerId,
                managerUsername,
                AssignmentType.MANUAL
        );
        assignment.setAssignmentStrategy("MANUAL");
        assignment.setAssignmentNotes(request.getAssignmentNote());
        Assignment savedAssignment = assignmentRepository.save(assignment);
        
        // Update ticket cache with assignment and priority
        ticket.setAssignedAgentId(agent.getAgentId());
        ticket.setAssignedAgentUsername(agent.getAgentUsername());
        ticket.setStatus("ASSIGNED");
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketCacheRepository.save(ticket);
        
        // Update agent workload
        agent.setActiveTickets(agent.getActiveTickets() + 1);
        agent.setTotalAssignedTickets(agent.getTotalAssignedTickets() + 1);
        agent.setLastAssignedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        
        // Update status based on workload
        if (agent.getActiveTickets() >= maxTicketsPerAgent * 0.8) {
            agent.setStatus(AgentStatus.BUSY);
        }
        
        agentWorkloadRepository.save(agent);
        
        //   Create SLA tracking with the priority manager set
        slaService.createSlaTracking(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                request.getPriority(),  //   Use priority from request
                ticket.getCategory()
        );
        
        // Publish TicketAssignedEvent to RabbitMQ
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                agent.getAgentId(),
                agent.getAgentUsername(),
                managerId,
                managerUsername,
                "MANUAL",
                LocalDateTime.now()
        );
        eventPublisher.publishTicketAssigned(event);
        
        log.info("Ticket {} manually assigned to {} with priority {} by {}", 
                ticket.getTicketNumber(), agent.getAgentUsername(), 
                request.getPriority(), managerUsername);
        
        return convertToAssignmentDTO(savedAssignment);
    }

    
    /**
     * Auto-assign ticket to best available agent
     */
    @Transactional
    public void autoAssignTicket(String ticketId) {
        if (!autoAssignEnabled) {
            log.info("Auto-assignment disabled, skipping ticket {}", ticketId);
            return;
        }
        
        // Get ticket
        Optional<TicketCache> ticketOpt = ticketCacheRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            log.warn("Ticket {} not found in cache for auto-assignment", ticketId);
            return;
        }
        
        TicketCache ticket = ticketOpt.get();
        
        // Check if already assigned
        if (ticket.getAssignedAgentId() != null) {
            log.info("Ticket {} already assigned, skipping auto-assignment", ticket.getTicketNumber());
            return;
        }

        syncAgentsFromAuthService();
        
        // Select best agent based on strategy
        AgentWorkload agent = selectBestAgent();
        if (agent == null) {
            log.warn("No available agents for auto-assignment of ticket {}", ticket.getTicketNumber());
            return;
        }
        
        // Create assignment
        Assignment assignment = new Assignment(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                agent.getAgentId(),
                agent.getAgentUsername(),
                "SYSTEM",
                "AutoAssignment",
                AssignmentType.AUTO
        );
        assignment.setAssignmentStrategy(assignmentStrategy);
        
        assignmentRepository.save(assignment);
        
        // Update ticket cache
        ticket.setAssignedAgentId(agent.getAgentId());
        ticket.setAssignedAgentUsername(agent.getAgentUsername());
        ticket.setStatus("ASSIGNED");
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketCacheRepository.save(ticket);
        
        // Update agent workload
        agent.setActiveTickets(agent.getActiveTickets() + 1);
        agent.setTotalAssignedTickets(agent.getTotalAssignedTickets() + 1);
        agent.setLastAssignedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        
        if (agent.getActiveTickets() >= maxTicketsPerAgent * 0.8) {
            agent.setStatus(AgentStatus.BUSY);
        }
        
        agentWorkloadRepository.save(agent);
        
        // Publish event
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                agent.getAgentId(),
                agent.getAgentUsername(),
                "SYSTEM",
                "AutoAssignment",
                "AUTO",
                LocalDateTime.now()
        );
        eventPublisher.publishTicketAssigned(event);
        
        log.info("Ticket {} auto-assigned to {} using {} strategy", 
                 ticket.getTicketNumber(), agent.getAgentUsername(), assignmentStrategy);
    }
    
    /**
     * Select best agent based on assignment strategy
     */
    private AgentWorkload selectBestAgent() {
        List<AgentWorkload> availableAgents = agentWorkloadRepository
                .findByStatusOrderByActiveTicketsAsc(AgentStatus.AVAILABLE);
        
        if (availableAgents.isEmpty()) {
            return null;
        }
        
        // For now, always use LEAST_LOADED (first in sorted list)
        return availableAgents.get(0);
    }
    
    /**
     * Get tickets assigned to an agent
     */
    public List<AssignmentDTO> getAgentTickets(String agentId) {
        List<Assignment> assignments = assignmentRepository
                .findByAgentIdAndStatus(agentId, AssignmentStatus.ACTIVE);
        
        return assignments.stream()
                .map(this::convertToAssignmentDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Complete assignment when ticket is resolved
     */
    @Transactional
    public void completeAssignment(String ticketId) {
        Optional<Assignment> assignmentOpt = assignmentRepository
                .findByTicketIdAndStatus(ticketId, AssignmentStatus.ACTIVE);
        
        if (assignmentOpt.isEmpty()) {
            return;
        }
        
        Assignment assignment = assignmentOpt.get();
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        
        // Update agent workload
        Optional<AgentWorkload> agentOpt = agentWorkloadRepository.findById(assignment.getAgentId());
        if (agentOpt.isPresent()) {
            AgentWorkload agent = agentOpt.get();
            agent.setActiveTickets(Math.max(0, agent.getActiveTickets() - 1));
            agent.setCompletedTickets(agent.getCompletedTickets() + 1);
            agent.setUpdatedAt(LocalDateTime.now());
            
            // Update status
            if (agent.getActiveTickets() < maxTicketsPerAgent * 0.8) {
                agent.setStatus(AgentStatus.AVAILABLE);
            }
            
            agentWorkloadRepository.save(agent);
        }
        
        log.info("Assignment completed for ticket {}", assignment.getTicketNumber());
    }
    
    // DTO Converters
    private UnassignedTicketDTO convertToUnassignedTicketDTO(TicketCache ticket) {
        UnassignedTicketDTO dto = new UnassignedTicketDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setCategory(ticket.getCategory());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setCreatedByUsername(ticket.getCreatedByUsername());
        dto.setCreatedAt(ticket.getCreatedAt());
        
        // Add SLA info
        Optional<SlaTracking> slaOpt = slaService.getSlaTracking(ticket.getTicketId());
        if (slaOpt.isPresent()) {
            SlaTracking sla = slaOpt.get();
            dto.setSlaStatus(sla.getSlaStatus().name());
            dto.setTimeRemaining(slaService.calculateTimeRemaining(sla));
        }
        
        return dto;
    }
    
    private AgentWorkloadDTO convertToAgentWorkloadDTO(AgentWorkload agent) {
        AgentWorkloadDTO dto = new AgentWorkloadDTO();
        dto.setAgentId(agent.getAgentId());
        dto.setAgentUsername(agent.getAgentUsername());
        dto.setActiveTickets(agent.getActiveTickets());
        dto.setTotalAssignedTickets(agent.getTotalAssignedTickets());
        dto.setCompletedTickets(agent.getCompletedTickets());
        dto.setStatus(agent.getStatus().name());
        dto.setLastAssignedAt(agent.getLastAssignedAt());
        dto.setIsRecommended(false);
        return dto;
    }
    
    private AssignmentDTO convertToAssignmentDTO(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setAssignmentId(assignment.getAssignmentId());
        dto.setTicketId(assignment.getTicketId());
        dto.setTicketNumber(assignment.getTicketNumber());
        dto.setAgentId(assignment.getAgentId());
        dto.setAgentUsername(assignment.getAgentUsername());
        dto.setAssignedBy(assignment.getAssignedBy());
        dto.setAssignedByUsername(assignment.getAssignedByUsername());
        dto.setAssignmentType(assignment.getAssignmentType().name());
        dto.setStatus(assignment.getStatus().name());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setCompletedAt(assignment.getCompletedAt());
        return dto;
    }

    @Transactional
    public void syncAgentsFromAuthService(){
        log.info("Getting Agents from auth service");
        List<AuthServiceClient.AgentDTO> agents= authServiceClient.getAllAgents();

        for(AuthServiceClient.AgentDTO agentDTO: agents){
            Optional<AgentWorkload> existingAgent=agentWorkloadRepository.findById(agentDTO.getUserId());

            if(existingAgent.isEmpty()){
                AgentWorkload newAgent=new AgentWorkload(
                    agentDTO.getUserId(),
                    agentDTO.getUsername()
                );
                newAgent.setStatus(AgentStatus.AVAILABLE);
                agentWorkloadRepository.save(newAgent);

                log.info("Got new Agent: {} ({})", agentDTO.getUsername(),agentDTO.getUserId());
            }
        }
        log.info("Toatal Agents {} and they are: {}", agents.size(), agents);
    }
        /**
     * Reassign ticket to different agent
     */
    @Transactional
    public void reassignTicket(String ticketId, String newAgentId, 
                            String managerId, String managerUsername) {
        log.info("Reassigning ticket {} to new agent {} by Manager {}", 
                ticketId, newAgentId, managerUsername);
        
        // Get ticket
        TicketCache ticket = ticketCacheRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found in cache"));
        
        String oldAgentId = ticket.getAssignedAgentId();
        
        if (oldAgentId == null) {
            throw new RuntimeException("Ticket is not assigned. Use manual assignment instead.");
        }
        
        if (oldAgentId.equals(newAgentId)) {
            throw new RuntimeException("Ticket is already assigned to this agent");
        }
        
        // Get old agent and decrement workload
        agentWorkloadRepository.findById(oldAgentId).ifPresent(oldAgent -> {
            oldAgent.setActiveTickets(Math.max(0, oldAgent.getActiveTickets() - 1));
            oldAgent.setUpdatedAt(LocalDateTime.now());
            
            // Update status
            if (oldAgent.getActiveTickets() < maxTicketsPerAgent * 0.8) {
                oldAgent.setStatus(AgentStatus.AVAILABLE);
            }
            
            agentWorkloadRepository.save(oldAgent);
        });
        
        // Get new agent
        AgentWorkload newAgent = agentWorkloadRepository.findById(newAgentId)
                .orElseThrow(() -> new RuntimeException("New agent not found"));
        
        // Update ticket
        ticket.setAssignedAgentId(newAgentId);
        ticket.setAssignedAgentUsername(newAgent.getAgentUsername());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketCacheRepository.save(ticket);
        
        // Update new agent workload
        newAgent.setActiveTickets(newAgent.getActiveTickets() + 1);
        newAgent.setTotalAssignedTickets(newAgent.getTotalAssignedTickets() + 1);
        newAgent.setLastAssignedAt(LocalDateTime.now());
        newAgent.setUpdatedAt(LocalDateTime.now());
        
        // Update status based on workload
        if (newAgent.getActiveTickets() >= maxTicketsPerAgent * 0.8) {
            newAgent.setStatus(AgentStatus.BUSY);
        }
        
        agentWorkloadRepository.save(newAgent);
        
        // Mark old assignment as completed
        assignmentRepository.findByTicketIdAndStatus(ticketId, AssignmentStatus.ACTIVE)
                .ifPresent(oldAssignment -> {
                    oldAssignment.setStatus(AssignmentStatus.COMPLETED);
                    oldAssignment.setCompletedAt(LocalDateTime.now());
                    assignmentRepository.save(oldAssignment);
                });
        
        // Create new assignment record
        Assignment newAssignment = new Assignment(
                ticketId,
                ticket.getTicketNumber(),
                newAgentId,
                newAgent.getAgentUsername(),
                managerId,
                managerUsername,
                AssignmentType.MANUAL
        );
        newAssignment.setAssignmentStrategy("MANUAL_REASSIGN");
        assignmentRepository.save(newAssignment);
        
        // Publish TicketAssignedEvent
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticketId,
                ticket.getTicketNumber(),
                newAgentId,
                newAgent.getAgentUsername(),
                managerId,
                managerUsername,
                "MANUAL_REASSIGN",
                LocalDateTime.now()
        );
        eventPublisher.publishTicketAssigned(event);
        
        log.info("Ticket {} reassigned from {} to {} by {}", 
                ticket.getTicketNumber(), oldAgentId, newAgent.getAgentUsername(), managerUsername);
    }


    
}
