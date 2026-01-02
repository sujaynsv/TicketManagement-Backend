package com.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Exchange
    public static final String EXCHANGE = "ticket.exchange";
    
    // Queue
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    
    // Routing Keys
    public static final String TICKET_CREATED_KEY = "ticket.created";
    public static final String TICKET_ASSIGNED_KEY = "ticket.assigned";
    public static final String TICKET_STATUS_CHANGED_KEY = "ticket.status.changed";
    public static final String COMMENT_ADDED_KEY = "comment.added";
    
    /**
     * Create exchange
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }
    
    /**
     * Create notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);  // durable = true
    }
    
    /**
     * Bind notification queue to exchange for all events
     */
    @Bean
    public Binding ticketCreatedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(TICKET_CREATED_KEY);
    }
    
    @Bean
    public Binding ticketAssignedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(TICKET_ASSIGNED_KEY);
    }
    
    @Bean
    public Binding ticketStatusChangedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(TICKET_STATUS_CHANGED_KEY);
    }
    
    @Bean
    public Binding commentAddedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(COMMENT_ADDED_KEY);
    }
    
    /**
     * JSON message converter with JSR310 support for LocalDateTime
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
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
