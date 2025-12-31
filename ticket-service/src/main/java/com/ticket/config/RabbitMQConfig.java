package com.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.name}")
    private String exchange;
    
    @Value("${rabbitmq.queue.ticket.name}")
    private String ticketQueueName;
    
    @Value("${rabbitmq.queue.notification.name}")
    private String notificationQueueName;
    
    @Value("${rabbitmq.routing-key.ticket-created}")
    private String ticketCreatedKey;
    
    @Value("${rabbitmq.routing-key.ticket-assigned}")
    private String ticketAssignedKey;
    
    @Value("${rabbitmq.routing-key.ticket-status-changed}")
    private String ticketStatusChangedKey;
    
    @Value("${rabbitmq.routing-key.comment-added}")
    private String commentAddedKey;
    
    /**
     * Create exchange
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }
    
    /**
     * Create ticket queue
     */
    @Bean
    public Queue ticketQueue() {
        return new Queue(ticketQueueName, true);
    }
    
    /**
     * Create notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return new Queue(notificationQueueName, true);
    }
    
    /**
     * Bind ticket queue to exchange for ticket.assigned events
     */
    @Bean
    public Binding ticketAssignedBinding(Queue ticketQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ticketQueue)
                .to(exchange)
                .with(ticketAssignedKey);
    }
    
    /**
     * Bind notification queue to various events
     */
    @Bean
    public Binding ticketCreatedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(ticketCreatedKey);
    }
    
    @Bean
    public Binding ticketAssignedNotificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(ticketAssignedKey);
    }
    
    @Bean
    public Binding ticketStatusChangedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(ticketStatusChangedKey);
    }
    
    @Bean
    public Binding commentAddedBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(commentAddedKey);
    }
    
    /**
     * JSON message converter with JSR310 support for LocalDateTime
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // ✅ Add this!
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
