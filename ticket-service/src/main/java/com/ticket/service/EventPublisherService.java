package com.ticket.service;

import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.event.TicketStatusChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {
    
    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.name}")
    private String ticketExchange;

    private static final String EXCHANGE="ticket.exchange";
    
    @Value("${rabbitmq.routing-key.ticket-created}")
    private String ticketCreatedRoutingKey;
    
    @Value("${rabbitmq.routing-key.ticket-status-changed}")
    private String ticketStatusChangedRoutingKey;
    
    @Value("${rabbitmq.routing-key.comment-added}")
    private String commentAddedRoutingKey;
    
    /**
     * Publish ticket created event
     */
    public void publishTicketCreated(TicketCreatedEvent event) {
        log.info("=== PUBLISHING TICKET CREATED EVENT ===");
        log.info("Exchange: {}", ticketExchange);
        log.info("Routing Key: {}", ticketCreatedRoutingKey);
        log.info("Ticket Number: {}", event.getTicketNumber());
        log.info("Event: {}", event);
        
        rabbitTemplate.convertAndSend(ticketExchange, ticketCreatedRoutingKey, event);
        
        log.info("  TicketCreatedEvent published successfully for: {}", event.getTicketNumber());
    }
    
    /**
     * Publish ticket status changed event
     */
    public void publishTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("=== PUBLISHING TICKET STATUS CHANGED EVENT ===");
        log.info("Exchange: {}", ticketExchange);
        log.info("Routing Key: {}", ticketStatusChangedRoutingKey);
        log.info("Ticket Number: {}", event.getTicketNumber());
        
        rabbitTemplate.convertAndSend(ticketExchange, ticketStatusChangedRoutingKey, event);
        
        log.info("  TicketStatusChangedEvent published for: {}", event.getTicketNumber());
    }
    
    /**
     * Publish comment added event
     */
    public void publishCommentAdded(CommentAddedEvent event) {
        log.info("=== PUBLISHING COMMENT ADDED EVENT ===");
        log.info("Exchange: {}", ticketExchange);
        log.info("Routing Key: {}", commentAddedRoutingKey);
        log.info("Ticket Number: {}", event.getTicketNumber());
        
        rabbitTemplate.convertAndSend(ticketExchange, commentAddedRoutingKey, event);
        
        log.info("  CommentAddedEvent published for ticket: {}", event.getTicketNumber());
    }

    public void publishTicketEscalated(TicketEscalatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "ticket.escalated", event);
            log.info("TicketEscalatedEvent published successfully for: {}", event.getTicketNumber());
        } catch (Exception e) {
            log.error("Failed to publish TicketEscalatedEvent: {}", e.getMessage(), e);
        }
    }

}
