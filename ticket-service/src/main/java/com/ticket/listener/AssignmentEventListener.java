package com.ticket.listener;

import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketAssignedEvent;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AssignmentEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(AssignmentEventListener.class);
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketActivityRepository ticketActivityRepository;
    
    /**
     * Listen to TicketAssignedEvent from Assignment Service
     * Update ticket with assigned agent details
     */
    @RabbitListener(queues = "ticket.queue")
    public void handleTicketAssigned(TicketAssignedEvent event) {
        try {
            log.info("Received TicketAssignedEvent: Ticket {} assigned to {}", 
                     event.getTicketNumber(), event.getAssignedToUsername());
            
            // Find ticket
            Optional<Ticket> ticketOpt = ticketRepository.findById(event.getTicketId());
            if (ticketOpt.isEmpty()) {
                log.warn("Ticket {} not found for assignment update", event.getTicketNumber());
                return;
            }
            
            Ticket ticket = ticketOpt.get();
            
            // Check if already assigned to this agent (idempotency)
            if (event.getAssignedToUserId().equals(ticket.getAssignedToUserId())) {
                log.info("Ticket {} already assigned to {}, skipping", 
                         event.getTicketNumber(), event.getAssignedToUsername());
                return;
            }
            
            // Update ticket
            ticket.setAssignedToUserId(event.getAssignedToUserId());
            ticket.setAssignedToUsername(event.getAssignedToUsername());
            ticket.setStatus(TicketStatus.ASSIGNED);
            ticket.setAssignedAt(event.getAssignedAt());
            ticket.setUpdatedAt(LocalDateTime.now());
            
            ticketRepository.save(ticket);
            log.info("Updated ticket {} with assignment to {}", 
                     event.getTicketNumber(), event.getAssignedToUsername());
            
            // Log activity
            String activityDescription;
            if ("MANUAL".equals(event.getAssignmentType())) {
                activityDescription = String.format("Ticket assigned to %s by %s", 
                        event.getAssignedToUsername(), event.getAssignedByUsername());
            } else {
                activityDescription = String.format("Ticket auto-assigned to %s", 
                        event.getAssignedToUsername());
            }
            
            TicketActivity activity = new TicketActivity(
                    event.getTicketId(),
                    "TICKET_ASSIGNED",
                    activityDescription,
                    event.getAssignedBy(),
                    event.getAssignedByUsername()
            );
            activity.setNewValue(event.getAssignedToUsername());
            
            ticketActivityRepository.save(activity);
            log.info("Logged activity for ticket assignment: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling TicketAssignedEvent for ticket {}: {}", 
                      event.getTicketNumber(), e.getMessage(), e);
        }
    }
}
