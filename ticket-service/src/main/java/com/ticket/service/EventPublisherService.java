package com.ticket.service;

import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {
    
    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
    
    private RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE_LOG_MSG = "Exchange: {}";
    private static final String ROUTING_KEY_LOG_MSG = "Routing Key: {}";
    private static final String TICKET_NUMBER_LOG_MSG = "Ticket Number: {}";

    public EventPublisherService(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate=rabbitTemplate;
    }
    
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
        log.info(EXCHANGE_LOG_MSG, ticketExchange);
        log.info(ROUTING_KEY_LOG_MSG, ticketCreatedRoutingKey);
        log.info(TICKET_NUMBER_LOG_MSG, event.getTicketNumber());
        log.info("Event: {}", event);
        
        rabbitTemplate.convertAndSend(ticketExchange, ticketCreatedRoutingKey, event);
        
        log.info("  TicketCreatedEvent published successfully for: {}", event.getTicketNumber());
    }
    
    /**
     * Publish ticket status changed event
     */
    public void publishTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("=== PUBLISHING TICKET STATUS CHANGED EVENT ===");
        log.info(EXCHANGE_LOG_MSG, ticketExchange);
        log.info(ROUTING_KEY_LOG_MSG, ticketStatusChangedRoutingKey);
        log.info(TICKET_NUMBER_LOG_MSG, event.getTicketNumber());
        
        rabbitTemplate.convertAndSend(ticketExchange, ticketStatusChangedRoutingKey, event);
        
        log.info("  TicketStatusChangedEvent published for: {}", event.getTicketNumber());
    }
    
    /**
     * Publish comment added event
     */
    public void publishCommentAdded(CommentAddedEvent event) {
        log.info("=== PUBLISHING COMMENT ADDED EVENT ===");
        log.info(EXCHANGE_LOG_MSG, ticketExchange);
        log.info(ROUTING_KEY_LOG_MSG, commentAddedRoutingKey);
        log.info(TICKET_NUMBER_LOG_MSG, event.getTicketNumber());
        
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
