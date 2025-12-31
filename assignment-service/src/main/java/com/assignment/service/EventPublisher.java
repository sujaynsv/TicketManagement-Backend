package com.assignment.service;

import com.ticket.event.TicketAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private static final String EXCHANGE = "ticket.exchange";
    
    /**
     * Publish TicketAssignedEvent to RabbitMQ
     */
    public void publishTicketAssigned(TicketAssignedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "ticket.assigned", event);
            log.info("Published TicketAssignedEvent: Ticket {} assigned to {}", 
                     event.getTicketNumber(), event.getAssignedToUsername());
        } catch (Exception e) {
            log.error("Failed to publish TicketAssignedEvent for ticket {}: {}", 
                      event.getTicketNumber(), e.getMessage());
        }
    }
}
