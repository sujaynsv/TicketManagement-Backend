package com.assignment.service;

import com.assignment.dto.*;
import com.assignment.entity.*;
import com.assignment.exception.AgentCapacityExceededException;
import com.assignment.exception.AgentOfflineException;
import com.assignment.exception.AssignmentNotFoundException;
import com.assignment.exception.AssignmentStatusException;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.ticket.event.TicketAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Lazy;
import java.util.*;

@Service
public class AdminAssignmentService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminAssignmentService.class);

    private static final String ASSIGNMENT_NOT_FOUND_MSG = "Assignment not found";
    
    private final AssignmentRepository assignmentRepository;
    
    private final TicketCacheRepository ticketCacheRepository;
    
    private final AgentWorkloadRepository agentWorkloadRepository;
    
    private final EventPublisher eventPublisher;
    
    private final SlaService slaService;

    // Self-injection for transactional proxy
    private final AdminAssignmentService self;

    public AdminAssignmentService(SlaService slaService, EventPublisher eventPublisher, AgentWorkloadRepository agentWorkloadRepository, TicketCacheRepository ticketCacheRepository, AssignmentRepository assignmentRepository, @Lazy AdminAssignmentService self){
        this.slaService=slaService;
        this.eventPublisher=eventPublisher;
        this.agentWorkloadRepository=agentWorkloadRepository;
        this.ticketCacheRepository=ticketCacheRepository;
        this.assignmentRepository=assignmentRepository;
        this.self = self;
    }

    // For Spring to inject self-reference
    
    @Value("${assignment.max-tickets-per-agent}")
    private Integer maxTicketsPerAgent;
    
    /**
     * Get all assignments with pagination and filtering
     */
    public Page<AdminAssignmentDTO> getAllAssignments(int page, int size, String status, 
                                                      String agentId, String ticketId, 
                                                      String assignmentType, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("assignedAt").descending());
        
        Specification<Assignment> spec = Specification.where(null);
        
        // Filter by status
        if (status != null && !status.isBlank()) {
            AssignmentStatus assignmentStatus = AssignmentStatus.valueOf(status.toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), assignmentStatus));
        }
        
        // Filter by agentId
        if (agentId != null && !agentId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agentId"), agentId));
        }
        
        // Filter by ticketId
        if (ticketId != null && !ticketId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ticketId"), ticketId));
        }
        
        // Filter by assignment type
        if (assignmentType != null && !assignmentType.isBlank()) {
            AssignmentType type = AssignmentType.valueOf(assignmentType.toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignmentType"), type));
        }
        
        // Search by ticket number or agent username
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("ticketNumber")), searchPattern),
                    cb.like(cb.lower(root.get("agentUsername")), searchPattern)
            ));
        }
        
        Page<Assignment> assignments = assignmentRepository.findAll(spec, pageable);
        return assignments.map(this::convertToAdminDTO);
    }
    
    /**
     * Get assignment by ID
     */
    public AdminAssignmentDTO getAssignmentById(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException(ASSIGNMENT_NOT_FOUND_MSG));
        return convertToAdminDTO(assignment);
    }
    
    /**
     * Force reassign ticket
     */
    @Transactional
    public AdminAssignmentDTO forceReassign(String assignmentId, AdminReassignRequest request,
                                           String adminId, String adminUsername) {
        log.info("Admin {} force reassigning assignment {}", adminUsername, assignmentId);
        
        Assignment oldAssignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException(ASSIGNMENT_NOT_FOUND_MSG));
        
        // Validate new agent
        AgentWorkload newAgent = agentWorkloadRepository.findById(request.newAgentId())
                .orElseThrow(() -> new RuntimeException("New agent not found"));
        
        if (newAgent.getStatus() == AgentStatus.OFFLINE) {
            throw new AgentOfflineException("Cannot assign to offline agent");
        }
        
        if (newAgent.getActiveTickets() >= maxTicketsPerAgent) {
            throw new AgentCapacityExceededException("Agent has reached maximum capacity");
        }
        
        // Get ticket
        TicketCache ticket = ticketCacheRepository.findById(oldAssignment.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        String oldAgentId = oldAssignment.getAgentId();
        
        // Update old agent workload
        agentWorkloadRepository.findById(oldAgentId).ifPresent(oldAgent -> {
            oldAgent.setActiveTickets(Math.max(0, oldAgent.getActiveTickets() - 1));
            oldAgent.setUpdatedAt(LocalDateTime.now());
            
            if (oldAgent.getActiveTickets() < maxTicketsPerAgent * 0.8) {
                oldAgent.setStatus(AgentStatus.AVAILABLE);
            }
            
            agentWorkloadRepository.save(oldAgent);
        });
        
        // Mark old assignment as REASSIGNED
        oldAssignment.setStatus(AssignmentStatus.REASSIGNED);
        oldAssignment.setCompletedAt(LocalDateTime.now());
        oldAssignment.setReassignmentReason(request.reason());
        assignmentRepository.save(oldAssignment);
        
        // Create new assignment
        Assignment newAssignment = new Assignment(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                newAgent.getAgentId(),
                newAgent.getAgentUsername(),
                adminId,
                adminUsername,
                AssignmentType.MANUAL
        );
        newAssignment.setAssignmentStrategy("ADMIN_REASSIGN");
        newAssignment.setStatus(AssignmentStatus.ASSIGNED);
        newAssignment.setTicketStatus("ASSIGNED");
        newAssignment.setPreviousAgentId(oldAgentId);
        newAssignment.setPreviousAgentUsername(oldAssignment.getAgentUsername());
        newAssignment.setReassignmentReason(request.reason());
        newAssignment.setAssignmentNotes("Force reassigned by admin: " + adminUsername);
        
        Assignment savedAssignment = assignmentRepository.save(newAssignment);
        
        // Update ticket
        ticket.setAssignedAgentId(newAgent.getAgentId());
        ticket.setAssignedAgentUsername(newAgent.getAgentUsername());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketCacheRepository.save(ticket);
        
        // Update new agent workload
        newAgent.setActiveTickets(newAgent.getActiveTickets() + 1);
        newAgent.setTotalAssignedTickets(newAgent.getTotalAssignedTickets() + 1);
        newAgent.setLastAssignedAt(LocalDateTime.now());
        newAgent.setUpdatedAt(LocalDateTime.now());
        
        if (newAgent.getActiveTickets() >= maxTicketsPerAgent * 0.8) {
            newAgent.setStatus(AgentStatus.BUSY);
        }
        
        agentWorkloadRepository.save(newAgent);
        
        // Publish event
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                newAgent.getAgentId(),
                newAgent.getAgentUsername(),
                adminId,
                adminUsername,
                "ADMIN_REASSIGN",
                LocalDateTime.now()
        );
        eventPublisher.publishTicketAssigned(event);
        
        log.info("Assignment {} force reassigned from {} to {}", 
                assignmentId, oldAgentId, newAgent.getAgentUsername());
        
        return convertToAdminDTO(savedAssignment);
    }
    
    /**
     * Unassign ticket (remove agent assignment)
     */
    @Transactional
    public String unassignTicket(String assignmentId, String reason, String adminId, String adminUsername) {
        log.info("Admin {} unassigning assignment {}", adminUsername, assignmentId);
        
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException(ASSIGNMENT_NOT_FOUND_MSG));
        
        // Allow unassigning ASSIGNED or REASSIGNED
        if (assignment.getStatus() != AssignmentStatus.ASSIGNED && 
            assignment.getStatus() != AssignmentStatus.REASSIGNED) {
            throw new AssignmentStatusException("Assignment status is " + assignment.getStatus() + 
                    ". Only ASSIGNED or REASSIGNED tickets can be unassigned.");
        }
        
        // If REASSIGNED, find the CURRENT assignment for this ticket and unassign that instead
        if (assignment.getStatus() == AssignmentStatus.REASSIGNED) {
            log.warn("Attempting to unassign a REASSIGNED assignment. Finding current assignment...");
            
            Optional<Assignment> currentAssignment = assignmentRepository
                    .findByTicketIdAndStatus(assignment.getTicketId(), AssignmentStatus.ASSIGNED);
            
            if (currentAssignment.isEmpty()) {
                throw new AssignmentNotFoundException("This is a historical assignment. No current assignment found for this ticket.");
            }
            
            // Recursively unassign the current one
            return self.unassignTicket(currentAssignment.get().getAssignmentId(), 
                                "Unassigned via historical assignment: " + reason, 
                                adminId, adminUsername);
        }
        
        // Original logic for ASSIGNED status
        agentWorkloadRepository.findById(assignment.getAgentId()).ifPresent(agent -> {
            agent.setActiveTickets(Math.max(0, agent.getActiveTickets() - 1));
            agent.setUpdatedAt(LocalDateTime.now());
            
            if (agent.getActiveTickets() < maxTicketsPerAgent * 0.8) {
                agent.setStatus(AgentStatus.AVAILABLE);
            }
            
            agentWorkloadRepository.save(agent);
        });
        
        assignment.setStatus(AssignmentStatus.NOT_ASSIGNED);
        assignment.setCompletedAt(LocalDateTime.now());
        assignment.setReassignmentReason("Unassigned by admin: " + reason);
        assignmentRepository.save(assignment);
        
        ticketCacheRepository.findById(assignment.getTicketId()).ifPresent(ticket -> {
            ticket.setAssignedAgentId(null);
            ticket.setAssignedAgentUsername(null);
            ticket.setStatus("OPEN");
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketCacheRepository.save(ticket);
        });
        
        log.info("Assignment {} unassigned by admin {}", assignmentId, adminUsername);
        
        return "Ticket unassigned successfully";
    }

    
    /**
     * Delete assignment (hard delete)
     */
    @Transactional
    public String deleteAssignment(String assignmentId, String adminId, String adminUsername) {
        log.warn("Admin {} deleting assignment {}", adminUsername, assignmentId);
        
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException(ASSIGNMENT_NOT_FOUND_MSG));
        
        // Only allow deletion of REASSIGNED or NOT_ASSIGNED
        if (assignment.getStatus() == AssignmentStatus.ASSIGNED) {
            throw new AssignmentStatusException("Cannot delete active assignment. Unassign it first.");
        }
        
        assignmentRepository.delete(assignment);
        
        log.info("Assignment {} deleted by admin {}", assignmentId, adminUsername);
        
        return "Assignment deleted successfully";
    }
    
    /**
     * Get assignment statistics
     */
    public AssignmentStatsDTO getAssignmentStats() {
        long totalAssignments = assignmentRepository.count();
        long activeAssignments = assignmentRepository.countByStatus(AssignmentStatus.ASSIGNED);
        long reassignedCount = assignmentRepository.countByStatus(AssignmentStatus.REASSIGNED);
        long notAssignedCount = assignmentRepository.countByStatus(AssignmentStatus.NOT_ASSIGNED);
        
        long autoAssignments = assignmentRepository.countByAssignmentType(AssignmentType.AUTO);
        long manualAssignments = assignmentRepository.countByAssignmentType(AssignmentType.MANUAL);
        
        long unassignedTickets = ticketCacheRepository.countByStatusAndAssignedAgentIdIsNull("OPEN");
        
        long totalAgents = agentWorkloadRepository.count();
        long availableAgents = agentWorkloadRepository.countByStatus(AgentStatus.AVAILABLE);
        long busyAgents = agentWorkloadRepository.countByStatus(AgentStatus.BUSY);
        long offlineAgents = agentWorkloadRepository.countByStatus(AgentStatus.OFFLINE);
        
        return new AssignmentStatsDTO(
                totalAssignments,
                activeAssignments,
                reassignedCount,
                notAssignedCount,
                autoAssignments,
                manualAssignments,
                unassignedTickets,
                totalAgents,
                availableAgents,
                busyAgents,
                offlineAgents
        );
    }
    
    /**
     * Get agent workload details
     */
    public AgentWorkloadDetailsDTO getAgentWorkload(String agentId) {
        AgentWorkload agent = agentWorkloadRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        
        List<Assignment> activeAssignments = assignmentRepository.findByAgentIdAndStatus(
                agentId, AssignmentStatus.ASSIGNED);
        
        long totalAssignments = assignmentRepository.countByAgentId(agentId);
        long completedAssignments = assignmentRepository.countByAgentIdAndStatusNot(
                agentId, AssignmentStatus.ASSIGNED);
        
        return new AgentWorkloadDetailsDTO(
                agent.getAgentId(),
                agent.getAgentUsername(),
                agent.getActiveTickets(),
                agent.getTotalAssignedTickets(),
                agent.getCompletedTickets(),
                agent.getStatus().name(),
                agent.getLastAssignedAt(),
                totalAssignments,
                completedAssignments,
                activeAssignments.stream()
                        .map(this::convertToAdminDTO)
                        .toList()
        );
    }
    
    /**
     * Get agent's active assignments
     */
    public List<AdminAssignmentDTO> getAgentActiveAssignments(String agentId) {
        List<Assignment> assignments = assignmentRepository.findByAgentIdAndStatus(
                agentId, AssignmentStatus.ASSIGNED);
        
        return assignments.stream()
                .map(this::convertToAdminDTO)
                .toList();
    }
    
    /**
     * Get unassigned tickets
     */
    public List<UnassignedTicketDTO> getUnassignedTickets() {
        List<TicketCache> tickets = ticketCacheRepository.findByStatusAndAssignedAgentIdIsNull("OPEN");
        
        return tickets.stream()
                .map(this::convertToUnassignedDTO)
                .toList();
    }
    
    /**
     * Get ticket assignment history
     */
    public List<AdminAssignmentDTO> getTicketAssignmentHistory(String ticketId) {
        List<Assignment> assignments = assignmentRepository.findByTicketIdOrderByAssignedAtDesc(ticketId);
        
        return assignments.stream()
                .map(this::convertToAdminDTO)
                .toList();
    }
    
    /**
     * Bulk reassign tickets
     */
    @Transactional
    public Map<String, Object> bulkReassign(BulkReassignRequest request, String adminId, String adminUsername) {
        log.info("Admin {} bulk reassigning from agent {} to {}", 
                adminUsername, request.fromAgentId(), request.toAgentId());
        
        // Validate agents
        AgentWorkload toAgent = agentWorkloadRepository.findById(request.toAgentId())
                .orElseThrow(() -> new RuntimeException("Target agent not found"));
        
        if (toAgent.getStatus() == AgentStatus.OFFLINE) {
            throw new AgentOfflineException("Cannot assign to offline agent");
        }
        
        // Get active assignments
        List<Assignment> assignments = assignmentRepository.findByAgentIdAndStatus(
                request.fromAgentId(), AssignmentStatus.ASSIGNED);
        
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Assignment assignment : assignments) {
            try {
                // Check capacity
                if (toAgent.getActiveTickets() >= maxTicketsPerAgent) {
                    errors.add("Agent reached maximum capacity at ticket: " + assignment.getTicketNumber());
                    failedCount++;
                    continue;
                }
                
                // Reassign
                AdminReassignRequest reassignRequest = new AdminReassignRequest(
                        toAgent.getAgentId(),
                        request.reason()
                );
                
                self.forceReassign(assignment.getAssignmentId(), reassignRequest, adminId, adminUsername);
                successCount++;
                
            } catch (Exception e) {
                errors.add("Failed to reassign " + assignment.getTicketNumber() + ": " + e.getMessage());
                failedCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalProcessed", assignments.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);
        
        log.info("Bulk reassign completed: {} success, {} failed", successCount, failedCount);
        
        return result;
    }
    
    /**
     * Convert to AdminAssignmentDTO
     */
    private AdminAssignmentDTO convertToAdminDTO(Assignment assignment) {
        // Get ticket details
        String ticketTitle = null;
        String priority = null;
        String category = null;
        
        ticketCacheRepository.findById(assignment.getTicketId()).ifPresent(ticket -> {
        });
        
        Optional<TicketCache> ticketOpt = ticketCacheRepository.findById(assignment.getTicketId());
        if (ticketOpt.isPresent()) {
            TicketCache ticket = ticketOpt.get();
            ticketTitle = ticket.getTitle();
            priority = ticket.getPriority();
            category = ticket.getCategory();
        }
        
        return new AdminAssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTicketId(),
                assignment.getTicketNumber(),
                ticketTitle,
                priority,
                category,
                assignment.getAgentId(),
                assignment.getAgentUsername(),
                assignment.getAssignedBy(),
                assignment.getAssignedByUsername(),
                assignment.getAssignmentType().name(),
                assignment.getAssignmentStrategy(),
                assignment.getPreviousAgentId(),
                assignment.getPreviousAgentUsername(),
                assignment.getReassignmentReason(),
                assignment.getAssignmentNotes(),
                assignment.getStatus().name(),
                assignment.getTicketStatus(),
                assignment.getAssignedAt(),
                assignment.getCompletedAt()
        );
    }
    
    /**
     * Convert to UnassignedTicketDTO
     */
    private UnassignedTicketDTO convertToUnassignedDTO(TicketCache ticket) {
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
        slaService.getSlaTracking(ticket.getTicketId()).ifPresent(sla -> {
            dto.setSlaStatus(sla.getSlaStatus().name());
            dto.setTimeRemaining(slaService.calculateTimeRemaining(sla));
        });
        
        return dto;
    }
}
