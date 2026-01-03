package com.assignment.listener;

import com.assignment.entity.TicketCache;
import com.assignment.repository.TicketCacheRepository;
import com.assignment.service.SlaService;
import com.ticket.event.TicketAssignedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {

    private static final String INVALID_TICKET_NUMBER_MSG = "Invalid event: ticketNumber is null";

    private final TicketCacheRepository ticketCacheRepository;
    private final SlaService slaService;

    @RabbitListener(queues = "assignment.ticket.created")
    @Transactional
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.info("Received TicketCreatedEvent: {}", event.getTicketNumber());
        
        try {
            if (event.getTicketNumber() == null) {
                log.error(INVALID_TICKET_NUMBER_MSG);
                return;
            }
            
            if (event.getTitle() == null) {
                log.warn("Event missing title for ticket: {}", event.getTicketNumber());
            }
            
            TicketCache ticketCache = new TicketCache();
            ticketCache.setTicketId(event.getTicketId());
            ticketCache.setTicketNumber(event.getTicketNumber());
            ticketCache.setTitle(event.getTitle() != null ? event.getTitle() : "No Title");
            ticketCache.setDescription(event.getDescription());
            ticketCache.setCategory(event.getCategory());
            ticketCache.setPriority(event.getPriority());
            ticketCache.setStatus("OPEN");
            ticketCache.setCreatedByUserId(event.getCreatedByUserId());
            ticketCache.setCreatedByUsername(event.getCreatedByUsername());
            ticketCache.setCreatedAt(event.getCreatedAt());
            ticketCache.setUpdatedAt(LocalDateTime.now());
            
            ticketCacheRepository.save(ticketCache);
            log.info("Ticket cached: {} (priority: {})", 
                    event.getTicketNumber(), 
                    event.getPriority() != null ? event.getPriority() : "Not set");
            
            if (event.getPriority() != null && !event.getPriority().trim().isEmpty()) {
                slaService.createSlaTracking(
                    event.getTicketId(),
                    event.getTicketNumber(),
                    event.getPriority(),
                    event.getCategory()
                );
            } else {
                log.info("SLA tracking will be created when manager assigns priority");
            }
            
        } catch (Exception e) {
            log.error("Error handling TicketCreatedEvent for ticket {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "assignment.ticket.assigned")
    @Transactional
    public void handleTicketAssigned(TicketAssignedEvent event) {
        log.info("Received TicketAssignedEvent: {}", event.getTicketNumber());
        
        try {
            if (event.getTicketNumber() == null) {
                log.error(INVALID_TICKET_NUMBER_MSG);
                return;
            }
            
            TicketCache ticketCache = ticketCacheRepository.findByTicketNumber(event.getTicketNumber())
                    .orElse(null);
            
            if (ticketCache == null) {
                log.warn("Ticket not found in cache: {} - skipping assignment update", 
                        event.getTicketNumber());
                return;
            }
            
            ticketCache.setAssignedAgentId(event.getAssignedToUserId());
            ticketCache.setAssignedAgentUsername(event.getAssignedToUsername());
            ticketCache.setStatus("ASSIGNED");
            ticketCache.setUpdatedAt(LocalDateTime.now());
            
            ticketCacheRepository.save(ticketCache);
            log.info("Ticket assigned: {} to {}", 
                    event.getTicketNumber(), event.getAssignedToUsername());
            
        } catch (Exception e) {
            log.error("Error handling TicketAssignedEvent for ticket {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "assignment.ticket.status")
    @Transactional
    public void handleTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("Received TicketStatusChangedEvent: {} to {}", 
                event.getTicketNumber(), event.getNewStatus());
        
        try {
            if (event.getTicketNumber() == null) {
                log.error(INVALID_TICKET_NUMBER_MSG);
                return;
            }
            
            TicketCache ticketCache = ticketCacheRepository.findByTicketNumber(event.getTicketNumber())
                    .orElse(null);
            
            if (ticketCache == null) {
                log.warn("Ticket not found in cache: {} - skipping status update", 
                        event.getTicketNumber());
                return;
            }
            
            String oldStatus = ticketCache.getStatus();
            ticketCache.setStatus(event.getNewStatus());
            ticketCache.setUpdatedAt(LocalDateTime.now());
            
            ticketCacheRepository.save(ticketCache);
            log.info("Ticket status updated: {} ({} to {})", 
                    event.getTicketNumber(), oldStatus, event.getNewStatus());
            
            if ("RESOLVED".equalsIgnoreCase(event.getNewStatus()) || 
                "CLOSED".equalsIgnoreCase(event.getNewStatus())) {
                slaService.recordResolution(ticketCache.getTicketId());
            }
            
        } catch (Exception e) {
            log.error("Error handling TicketStatusChangedEvent for ticket {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "assignment.ticket.escalated")
    @Transactional
    public void handleTicketEscalated(TicketEscalatedEvent event) {
        log.info("Received TicketEscalatedEvent: {} escalated to {}", 
                event.getTicketNumber(), event.getEscalatedToUsername());
        
        try {
            if (event.getTicketNumber() == null) {
                log.error(INVALID_TICKET_NUMBER_MSG);
                return;
            }
            
            TicketCache ticketCache = ticketCacheRepository.findByTicketNumber(event.getTicketNumber())
                    .orElse(null);
            
            if (ticketCache == null) {
                log.warn("Ticket not found in cache: {} - skipping escalation update", 
                        event.getTicketNumber());
                return;
            }
            
            ticketCache.setAssignedAgentId(event.getEscalatedToUserId());
            ticketCache.setAssignedAgentUsername(event.getEscalatedToUsername());
            ticketCache.setStatus("ESCALATED");
            ticketCache.setUpdatedAt(LocalDateTime.now());
            
            ticketCacheRepository.save(ticketCache);
            log.info("Ticket escalated in cache: {} to manager {}", 
                    event.getTicketNumber(), event.getEscalatedToUsername());
            
        } catch (Exception e) {
            log.error("Error handling TicketEscalatedEvent for ticket {}: {}", 
                    event.getTicketNumber(), e.getMessage(), e);
        }
    }

}
