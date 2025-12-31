package com.assignment.listener;

import com.assignment.entity.TicketCache;
import com.assignment.repository.TicketCacheRepository;
import com.assignment.service.AssignmentService;
import com.assignment.service.SlaService;
import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class TicketEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(TicketEventListener.class);
    
    @Autowired
    private TicketCacheRepository ticketCacheRepository;
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private SlaService slaService;
    
    /**
     * Listen to TicketCreatedEvent
     * - Store ticket in cache
     * - Create SLA tracking
     * - Auto-assign if enabled
     */
    @RabbitListener(queues = "assignment.queue")
    public void handleTicketCreated(TicketCreatedEvent event) {
        try {
            log.info("Received TicketCreatedEvent: {}", event.getTicketNumber());
            
            // Check if already exists (idempotency)
            Optional<TicketCache> existingTicket = ticketCacheRepository.findById(event.getTicketId());
            if (existingTicket.isPresent()) {
                log.info("Ticket {} already exists in cache, skipping", event.getTicketNumber());
                return;
            }
            
            // Store ticket in cache
            TicketCache ticketCache = new TicketCache();
            ticketCache.setTicketId(event.getTicketId());
            ticketCache.setTicketNumber(event.getTicketNumber());
            ticketCache.setTitle(event.getTitle());
            ticketCache.setDescription(event.getDescription());
            ticketCache.setCategory(event.getCategory());
            ticketCache.setPriority(event.getPriority());
            ticketCache.setStatus("OPEN");
            ticketCache.setCreatedByUserId(event.getCreatedByUserId());
            ticketCache.setCreatedByUsername(event.getCreatedByUsername());
            ticketCache.setCreatedAt(event.getCreatedAt());
            ticketCache.setUpdatedAt(LocalDateTime.now());
            
            ticketCacheRepository.save(ticketCache);
            log.info("Stored ticket {} in cache", event.getTicketNumber());
            
            // Create SLA tracking
            slaService.createSlaTracking(
                    event.getTicketId(),
                    event.getTicketNumber(),
                    event.getPriority(),
                    event.getCategory()
            );
            log.info("Created SLA tracking for ticket {}", event.getTicketNumber());
            
            // Auto-assign ticket
            assignmentService.autoAssignTicket(event.getTicketId());
            
        } catch (Exception e) {
            log.error("Error handling TicketCreatedEvent for ticket {}: {}", 
                      event.getTicketNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Listen to CommentAddedEvent
     * - Update SLA tracking (first response)
     */
    @RabbitListener(queues = "assignment.queue")
    public void handleCommentAdded(CommentAddedEvent event) {
        try {
            log.info("Received CommentAddedEvent for ticket {}", event.getTicketNumber());
            
            // Only track first response by agent (not customer comments)
            if (event.getIsInternal() != null && !event.getIsInternal()) {
                // Check if comment is from agent (not customer)
                Optional<TicketCache> ticketOpt = ticketCacheRepository.findById(event.getTicketId());
                if (ticketOpt.isPresent()) {
                    TicketCache ticket = ticketOpt.get();
                    
                    // If ticket is assigned and comment is from agent, record first response
                    if (ticket.getAssignedAgentId() != null && 
                        event.getUserId().equals(ticket.getAssignedAgentId())) {
                        
                        slaService.recordFirstResponse(event.getTicketId());
                        log.info("Recorded first response for ticket {}", event.getTicketNumber());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling CommentAddedEvent for ticket {}: {}", 
                      event.getTicketNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Listen to TicketStatusChangedEvent
     * - Update ticket cache
     * - Update SLA tracking (if resolved)
     * - Complete assignment (if resolved/closed)
     */
    @RabbitListener(queues = "assignment.queue")
    public void handleTicketStatusChanged(TicketStatusChangedEvent event) {
        try {
            log.info("Received TicketStatusChangedEvent for ticket {}: {} -> {}", 
                     event.getTicketNumber(), event.getOldStatus(), event.getNewStatus());
            
            // Update ticket cache
            Optional<TicketCache> ticketOpt = ticketCacheRepository.findById(event.getTicketId());
            if (ticketOpt.isPresent()) {
                TicketCache ticket = ticketOpt.get();
                ticket.setStatus(event.getNewStatus());
                ticket.setUpdatedAt(LocalDateTime.now());
                ticketCacheRepository.save(ticket);
                log.info("Updated ticket cache for {}", event.getTicketNumber());
            }
            
            // If ticket is resolved, update SLA tracking and complete assignment
            if ("RESOLVED".equals(event.getNewStatus()) || "CLOSED".equals(event.getNewStatus())) {
                slaService.recordResolution(event.getTicketId());
                assignmentService.completeAssignment(event.getTicketId());
                log.info("Completed assignment for ticket {}", event.getTicketNumber());
            }
            
        } catch (Exception e) {
            log.error("Error handling TicketStatusChangedEvent for ticket {}: {}", 
                      event.getTicketNumber(), e.getMessage(), e);
        }
    }
}
