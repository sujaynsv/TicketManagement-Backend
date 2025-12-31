package com.assignment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {
    
    // Exchange
    public static final String EXCHANGE = "ticket.exchange";
    
    // Queues
    public static final String ASSIGNMENT_QUEUE = "assignment.queue";
    
    // Routing Keys
    public static final String TICKET_CREATED_KEY = "ticket.created";
    public static final String TICKET_ASSIGNED_KEY = "ticket.assigned";
    public static final String COMMENT_ADDED_KEY = "comment.added";
    public static final String TICKET_STATUS_CHANGED_KEY = "ticket.status.changed";
    
    /**
     * Create exchange
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }
    
    /**
     * Create assignment queue
     */
    @Bean
    public Queue assignmentQueue() {
        return new Queue(ASSIGNMENT_QUEUE, true);  // durable = true
    }
    
    /**
     * Bind assignment queue to exchange for ticket.created events
     */
    @Bean
    public Binding ticketCreatedBinding(Queue assignmentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(assignmentQueue)
                .to(exchange)
                .with(TICKET_CREATED_KEY);
    }
    
    /**
     * Bind assignment queue to exchange for comment.added events
     */
    @Bean
    public Binding commentAddedBinding(Queue assignmentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(assignmentQueue)
                .to(exchange)
                .with(COMMENT_ADDED_KEY);
    }
    
    /**
     * Bind assignment queue to exchange for ticket.status.changed events
     */
    @Bean
    public Binding ticketStatusChangedBinding(Queue assignmentQueue, TopicExchange exchange) {
        return BindingBuilder.bind(assignmentQueue)
                .to(exchange)
                .with(TICKET_STATUS_CHANGED_KEY);
    }
    
    /**
     * JSON message converter
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper=new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    
    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
