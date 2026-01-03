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
    
    private final TicketRepository ticketRepository;
    private final TicketActivityRepository ticketActivityRepository;
    private final EventPublisherService eventPublisher;
    private final MongoTemplate mongoTemplate;

    private static final String TICKETNOTFOUNDMESSAGE="Ticket Not Found!";
    
    //     FIX 1: Constructor injection instead of field injection
    public AdminTicketService(TicketRepository ticketRepository,
                             TicketActivityRepository ticketActivityRepository,
                             EventPublisherService eventPublisher,
                             MongoTemplate mongoTemplate) {
        this.ticketRepository = ticketRepository;
        this.ticketActivityRepository = ticketActivityRepository;
        this.eventPublisher = eventPublisher;
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Get all tickets with pagination and filtering
     *     FIX 2 & 3: Reduced parameters by using TicketFilterRequest DTO
     */
    public Page<AdminTicketDTO> getAllTickets(TicketFilterRequest filterRequest) {
        Pageable pageable = PageRequest.of(
            filterRequest.page(), 
            filterRequest.size(), 
            Sort.by("createdAt").descending()
        );
        
        Query query = buildTicketQuery(filterRequest);
        query.with(pageable);
        
        List<Ticket> tickets = mongoTemplate.find(query, Ticket.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Ticket.class);
        
        Page<Ticket> ticketPage = PageableExecutionUtils.getPage(tickets, pageable, () -> count);
        
        return ticketPage.map(this::convertToAdminDTO);
    }
    
    /**
     *     FIX 2: Extracted method to reduce cognitive complexity
     * Build MongoDB query from filter request
     */
    private Query buildTicketQuery(TicketFilterRequest filterRequest) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        
        addStatusCriteria(criteria, filterRequest.status());
        addPriorityCriteria(criteria, filterRequest.priority());
        addCategoryCriteria(criteria, filterRequest.category());
        addAssignedUserCriteria(criteria, filterRequest.assignedToUserId());
        addCreatedByCriteria(criteria, filterRequest.createdByUserId());
        addSearchCriteria(criteria, filterRequest.search());
        
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        
        return query;
    }
    
    /**
     * Add status filter criteria
     */
    private void addStatusCriteria(List<Criteria> criteria, String status) {
        if (status != null && !status.isBlank()) {
            try {
                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
                criteria.add(Criteria.where("status").is(ticketStatus));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
    }
    
    /**
     * Add priority filter criteria
     */
    private void addPriorityCriteria(List<Criteria> criteria, String priority) {
        if (priority != null && !priority.isBlank()) {
            try {
                TicketPriority ticketPriority = TicketPriority.valueOf(priority.toUpperCase());
                criteria.add(Criteria.where("priority").is(ticketPriority));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid priority: {}", priority);
            }
        }
    }
    
    /**
     * Add category filter criteria
     */
    private void addCategoryCriteria(List<Criteria> criteria, String category) {
        if (category != null && !category.isBlank()) {
            try {
                TicketCategory ticketCategory = TicketCategory.valueOf(category.toUpperCase());
                criteria.add(Criteria.where("category").is(ticketCategory));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category: {}", category);
            }
        }
    }
    
    /**
     * Add assigned user filter criteria
     */
    private void addAssignedUserCriteria(List<Criteria> criteria, String assignedToUserId) {
        if (assignedToUserId != null && !assignedToUserId.isBlank()) {
            criteria.add(Criteria.where("assignedToUserId").is(assignedToUserId));
        }
    }
    
    /**
     * Add created by filter criteria
     */
    private void addCreatedByCriteria(List<Criteria> criteria, String createdByUserId) {
        if (createdByUserId != null && !createdByUserId.isBlank()) {
            criteria.add(Criteria.where("createdByUserId").is(createdByUserId));
        }
    }
    
    /**
     * Add search criteria
     */
    private void addSearchCriteria(List<Criteria> criteria, String search) {
        if (search != null && !search.isBlank()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("ticketNumber").regex(search, "i"),
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            );
            criteria.add(searchCriteria);
        }
    }
    
    /**
     * Get ticket by ID
     */
    
    public AdminTicketDTO getTicketById(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(TICKETNOTFOUNDMESSAGE));
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
                .orElseThrow(() -> new RuntimeException(TICKETNOTFOUNDMESSAGE));
        
        TicketPriority oldPriority = ticket.getPriority();
        TicketPriority newPriority = TicketPriority.fromString(request.priority());
        
        ticket.setPriority(newPriority);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        String activityMessage = buildPriorityChangeMessage(oldPriority, newPriority, request.reason());
        
        logActivity(ticketId, "PRIORITY_CHANGED", activityMessage, adminId, adminUsername);
        
        log.info("Priority changed for ticket {}: {} -> {}", 
                ticket.getTicketNumber(), oldPriority, newPriority);
        
        return convertToAdminDTO(updatedTicket);
    }
    
    /**
     * Build priority change message
     */
    private String buildPriorityChangeMessage(TicketPriority oldPriority, 
                                              TicketPriority newPriority, 
                                              String reason) {
        if (oldPriority == null) {
            return String.format("Priority set to %s by admin. Reason: %s", 
                    newPriority.name(), reason);
        }
        return String.format("Priority changed from %s to %s by admin. Reason: %s", 
                oldPriority.name(), newPriority.name(), reason);
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
                .orElseThrow(() -> new RuntimeException(TICKETNOTFOUNDMESSAGE));
        
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
                .orElseThrow(() -> new RuntimeException(TICKETNOTFOUNDMESSAGE));
        
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = TicketStatus.valueOf(request.status().toUpperCase());
        
        // Admin can force any status change (no validation)
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        updateTicketTimestamps(ticket, newStatus);
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        String activityMessage = String.format("Status force changed from %s to %s by admin. Reason: %s", 
                oldStatus.name(), newStatus.name(), request.reason());
        
        logActivity(ticketId, "STATUS_FORCE_CHANGED", activityMessage, adminId, adminUsername);
        
        // Publish event
        publishStatusChangedEvent(ticket, oldStatus, newStatus, adminId, adminUsername, request.reason());
        
        log.info("Status force changed for ticket {}: {} -> {}", 
                ticket.getTicketNumber(), oldStatus, newStatus);
        
        return convertToAdminDTO(updatedTicket);
    }
    
    /**
     * Update ticket timestamps based on status
     */
    private void updateTicketTimestamps(Ticket ticket, TicketStatus newStatus) {
        if (newStatus == TicketStatus.ASSIGNED && ticket.getAssignedAt() == null) {
            ticket.setAssignedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
    }
    
    /**
     * Publish ticket status changed event
     */
    private void publishStatusChangedEvent(Ticket ticket, TicketStatus oldStatus, 
                                          TicketStatus newStatus, String adminId, 
                                          String adminUsername, String reason) {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                oldStatus.name(),
                newStatus.name(),
                adminId,
                adminUsername,
                "Admin force changed: " + reason,
                LocalDateTime.now()
        );
        eventPublisher.publishTicketStatusChanged(event);
    }
    
    /**
     * Delete ticket
     */
    @Transactional
    public String deleteTicket(String ticketId, boolean hardDelete, String adminId, String adminUsername) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(TICKETNOTFOUNDMESSAGE));
        
        if (hardDelete) {
            return performHardDelete(ticket, adminUsername);
        }
        return performSoftDelete(ticket, ticketId, adminId, adminUsername);
    }
    
    /**
     * Perform hard delete
     */
    private String performHardDelete(Ticket ticket, String adminUsername) {
        ticketRepository.delete(ticket);
        log.warn("Admin {} hard deleted ticket {}", adminUsername, ticket.getTicketNumber());
        return "Ticket permanently deleted";
    }
    
    /**
     * Perform soft delete
     */
    private String performSoftDelete(Ticket ticket, String ticketId, String adminId, String adminUsername) {
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        
        logActivity(ticketId, "TICKET_DELETED", 
                "Ticket soft deleted by admin", adminId, adminUsername);
        
        log.info("Admin {} soft deleted ticket {}", adminUsername, ticket.getTicketNumber());
        return "Ticket closed (soft delete)";
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
        long criticalTickets = countTicketsByPriority(TicketPriority.CRITICAL);
        long highPriorityTickets = countTicketsByPriority(TicketPriority.HIGH);
        long mediumPriorityTickets = countTicketsByPriority(TicketPriority.MEDIUM);
        long lowPriorityTickets = countTicketsByPriority(TicketPriority.LOW);
        long noPriorityTickets = countTicketsByPriority(null);
        
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
     * Count tickets by priority
     */
    private long countTicketsByPriority(TicketPriority priority) {
        Query query = new Query(Criteria.where("priority").is(priority));
        return mongoTemplate.count(query, Ticket.class);
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
