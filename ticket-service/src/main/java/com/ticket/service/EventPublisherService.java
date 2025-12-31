package com.ticket.service;

import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.name}")
    private String ticketExchange;
    
    @Value("${rabbitmq.routing-key.ticket-created}")
    private String ticketCreatedRoutingKey;
    
    @Value("${rabbitmq.routing-key.ticket-updated}")
    private String ticketUpdatedRoutingKey;
    
    @Value("${rabbitmq.routing-key.comment-added}")
    private String commentAddedRoutingKey;
    
    /**
     * Publish ticket created event
     */
    public void publishTicketCreated(TicketCreatedEvent event) {
        rabbitTemplate.convertAndSend(ticketExchange, ticketCreatedRoutingKey, event);
        System.out.println("Published TicketCreatedEvent: " + event.getTicketNumber());
    }
    
    /**
     * Publish ticket status changed event
     */
    public void publishTicketStatusChanged(TicketStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(ticketExchange, ticketUpdatedRoutingKey, event);
        System.out.println("Published TicketStatusChangedEvent: " + event.getTicketNumber());
    }
    
    /**
     * Publish comment added event
     */
    public void publishCommentAdded(CommentAddedEvent event) {
        rabbitTemplate.convertAndSend(ticketExchange, commentAddedRoutingKey, event);
        System.out.println("Published CommentAddedEvent for ticket: " + event.getTicketNumber());
    }
}
