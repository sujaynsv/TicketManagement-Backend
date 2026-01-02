package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketPriority;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketStatusChangedEvent;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminTicketService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminTicketService.class);
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketActivityRepository ticketActivityRepository;
    
    @Autowired
    private EventPublisherService eventPublisher;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * Get all tickets with pagination and filtering
     */
    public Page<AdminTicketDTO> getAllTickets(int page, int size, String status, String priority,
                                              String category, String assignedToUserId, 
                                              String createdByUserId, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        
        // Filter by status
        if (status != null && !status.isBlank()) {
            try {
                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
                criteria.add(Criteria.where("status").is(ticketStatus));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
        
        // Filter by priority
        if (priority != null && !priority.isBlank()) {
            try {
                TicketPriority ticketPriority = TicketPriority.valueOf(priority.toUpperCase());
                criteria.add(Criteria.where("priority").is(ticketPriority));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid priority: {}", priority);
            }
        }
        
        // Filter by category
        if (category != null && !category.isBlank()) {
            try {
                TicketCategory ticketCategory = TicketCategory.valueOf(category.toUpperCase());
                criteria.add(Criteria.where("category").is(ticketCategory));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category: {}", category);
            }
        }
        
        // Filter by assigned user
        if (assignedToUserId != null && !assignedToUserId.isBlank()) {
            criteria.add(Criteria.where("assignedToUserId").is(assignedToUserId));
        }
        
        // Filter by creator
        if (createdByUserId != null && !createdByUserId.isBlank()) {
            criteria.add(Criteria.where("createdByUserId").is(createdByUserId));
        }
        
        // Search by ticket number, title, or description
        if (search != null && !search.isBlank()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("ticketNumber").regex(search, "i"),
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            );
            criteria.add(searchCriteria);
        }
        
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        
        query.with(pageable);
        
        List<Ticket> tickets = mongoTemplate.find(query, Ticket.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Ticket.class);
        
        Page<Ticket> ticketPage = PageableExecutionUtils.getPage(tickets, pageable, () -> count);
        
        return ticketPage.map(this::convertToAdminDTO);
    }
    
    /**
     * Get ticket by ID
     */
    public AdminTicketDTO getTicketById(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return convertToAdminDTO(ticket);
    }
    
    /**
     * Change ticket priority
     */
    @Transactional
    public AdminTicketDTO changePriority(String ticketId, AdminChangePriorityRequest request,
                                        String adminId, String adminUsername) {
        log.info("Admin {} changing priority for ticket {} to {}", 
                adminUsername, ticketId, request.priority());
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        TicketPriority oldPriority = ticket.getPriority();
        TicketPriority newPriority = TicketPriority.fromString(request.priority());
        
        ticket.setPriority(newPriority);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        String activityMessage = oldPriority == null 
                ? String.format("Priority set to %s by admin. Reason: %s", 
                        newPriority.name(), request.reason())
                : String.format("Priority changed from %s to %s by admin. Reason: %s", 
                        oldPriority.name(), newPriority.name(), request.reason());
        
        logActivity(ticketId, "PRIORITY_CHANGED", activityMessage, adminId, adminUsername);
        
        log.info("Priority changed for ticket {}: {} -> {}", 
                ticket.getTicketNumber(), oldPriority, newPriority);
        
        return convertToAdminDTO(updatedTicket);
    }
    
    /**
     * Change ticket category
     */
    @Transactional
    public AdminTicketDTO changeCategory(String ticketId, AdminChangeCategoryRequest request,
                                        String adminId, String adminUsername) {
        log.info("Admin {} changing category for ticket {} to {}", 
                adminUsername, ticketId, request.category());
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        TicketCategory oldCategory = ticket.getCategory();
        TicketCategory newCategory = TicketCategory.fromString(request.category());
        
        ticket.setCategory(newCategory);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        String activityMessage = String.format("Category changed from %s to %s by admin. Reason: %s", 
                oldCategory.name(), newCategory.name(), request.reason());
        
        logActivity(ticketId, "CATEGORY_CHANGED", activityMessage, adminId, adminUsername);
        
        log.info("Category changed for ticket {}: {} -> {}", 
                ticket.getTicketNumber(), oldCategory, newCategory);
        
        return convertToAdminDTO(updatedTicket);
    }
    
    /**
     * Change ticket status (force change, bypass validation)
     */
    @Transactional
    public AdminTicketDTO changeStatus(String ticketId, AdminChangeStatusRequest request,
                                      String adminId, String adminUsername) {
        log.info("Admin {} force changing status for ticket {} to {}", 
                adminUsername, ticketId, request.status());
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = TicketStatus.valueOf(request.status().toUpperCase());
        
        // Admin can force any status change (no validation)
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        // Update timestamps based on status
        if (newStatus == TicketStatus.ASSIGNED && ticket.getAssignedAt() == null) {
            ticket.setAssignedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        String activityMessage = String.format("Status force changed from %s to %s by admin. Reason: %s", 
                oldStatus.name(), newStatus.name(), request.reason());
        
        logActivity(ticketId, "STATUS_FORCE_CHANGED", activityMessage, adminId, adminUsername);
        
        // Publish event
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                ticketId,
                ticket.getTicketNumber(),
                oldStatus.name(),
                newStatus.name(),
                adminId,
                adminUsername,
                "Admin force changed: " + request.reason(),
                LocalDateTime.now()
        );
        eventPublisher.publishTicketStatusChanged(event);
        
        log.info("Status force changed for ticket {}: {} -> {}", 
                ticket.getTicketNumber(), oldStatus, newStatus);
        
        return convertToAdminDTO(updatedTicket);
    }
    
    /**
     * Delete ticket
     */
    @Transactional
    public String deleteTicket(String ticketId, boolean hardDelete, String adminId, String adminUsername) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        if (hardDelete) {
            // Hard delete - permanently remove from database
            ticketRepository.delete(ticket);
            log.warn("Admin {} hard deleted ticket {}", adminUsername, ticket.getTicketNumber());
            return "Ticket permanently deleted";
        } else {
            // Soft delete - change status to CLOSED
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
            
            logActivity(ticketId, "TICKET_DELETED", 
                    "Ticket soft deleted by admin", adminId, adminUsername);
            
            log.info("Admin {} soft deleted ticket {}", adminUsername, ticket.getTicketNumber());
            return "Ticket closed (soft delete)";
        }
    }
    
    /**
     * Get ticket statistics
     */
    public TicketStatsDTO getTicketStats() {
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long assignedTickets = ticketRepository.countByStatus(TicketStatus.ASSIGNED);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long resolvedTickets = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByStatus(TicketStatus.CLOSED);
        long escalatedTickets = ticketRepository.countByStatus(TicketStatus.ESCALATED);
        
        // Count by priority
        Query criticalQuery = new Query(Criteria.where("priority").is(TicketPriority.CRITICAL));
        Query highQuery = new Query(Criteria.where("priority").is(TicketPriority.HIGH));
        Query mediumQuery = new Query(Criteria.where("priority").is(TicketPriority.MEDIUM));
        Query lowQuery = new Query(Criteria.where("priority").is(TicketPriority.LOW));
        Query noPriorityQuery = new Query(Criteria.where("priority").is(null));
        
        long criticalTickets = mongoTemplate.count(criticalQuery, Ticket.class);
        long highPriorityTickets = mongoTemplate.count(highQuery, Ticket.class);
        long mediumPriorityTickets = mongoTemplate.count(mediumQuery, Ticket.class);
        long lowPriorityTickets = mongoTemplate.count(lowQuery, Ticket.class);
        long noPriorityTickets = mongoTemplate.count(noPriorityQuery, Ticket.class);
        
        return new TicketStatsDTO(
                totalTickets,
                openTickets,
                assignedTickets,
                inProgressTickets,
                resolvedTickets,
                closedTickets,
                escalatedTickets,
                criticalTickets,
                highPriorityTickets,
                mediumPriorityTickets,
                lowPriorityTickets,
                noPriorityTickets
        );
    }
    
    /**
     * Get user's tickets (created by user)
     */
    public Page<AdminTicketDTO> getUserTickets(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Query query = new Query(Criteria.where("createdByUserId").is(userId));
        query.with(pageable);
        
        List<Ticket> tickets = mongoTemplate.find(query, Ticket.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Ticket.class);
        
        Page<Ticket> ticketPage = PageableExecutionUtils.getPage(tickets, pageable, () -> count);
        
        return ticketPage.map(this::convertToAdminDTO);
    }
    
    /**
     * Get agent's assigned tickets
     */
    public Page<AdminTicketDTO> getAgentTickets(String agentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("assignedAt").descending());
        
        Query query = new Query(Criteria.where("assignedToUserId").is(agentId));
        query.with(pageable);
        
        List<Ticket> tickets = mongoTemplate.find(query, Ticket.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Ticket.class);
        
        Page<Ticket> ticketPage = PageableExecutionUtils.getPage(tickets, pageable, () -> count);
        
        return ticketPage.map(this::convertToAdminDTO);
    }
    
    /**
     * Log activity
     */
    private void logActivity(String ticketId, String activityType, String description,
                            String userId, String username) {
        TicketActivity activity = new TicketActivity(
                ticketId, activityType, description, userId, username
        );
        ticketActivityRepository.save(activity);
    }
    
    /**
     * Convert to AdminTicketDTO
     */
    private AdminTicketDTO convertToAdminDTO(Ticket ticket) {
        return new AdminTicketDTO(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus() != null ? ticket.getStatus().name() : null,
                ticket.getCategory() != null ? ticket.getCategory().name() : null,
                ticket.getPriority() != null ? ticket.getPriority().name() : null,
                ticket.getCreatedByUserId(),
                ticket.getCreatedByUsername(),
                ticket.getAssignedToUserId(),
                ticket.getAssignedToUsername(),
                ticket.getEscalatedToUserId(),
                ticket.getEscalatedToUsername(),
                ticket.getEscalationType() != null ? ticket.getEscalationType().name() : null,
                ticket.getTags(),
                ticket.getCommentCount(),
                ticket.getAttachmentCount(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getAssignedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getEscalatedAt()
        );
    }
}
