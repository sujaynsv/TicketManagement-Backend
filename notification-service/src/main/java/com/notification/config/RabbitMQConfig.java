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
    
    public static final String EXCHANGE = "ticket.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    
    public static final String TICKET_CREATED_KEY = "ticket.created";
    public static final String TICKET_ASSIGNED_KEY = "ticket.assigned";
    public static final String TICKET_STATUS_CHANGED_KEY = "ticket.status.changed";
    public static final String COMMENT_ADDED_KEY = "comment.added";
    public static final String SLA_WARNING_KEY = "sla.warning";
    public static final String SLA_BREACH_KEY = "sla.breach";
    
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }
    
    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }
    
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
    
    @Bean
    public Binding slaWarningBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(SLA_WARNING_KEY);
    }
    
    @Bean
    public Binding slaBreachBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(exchange)
                .with(SLA_BREACH_KEY);
    }
    
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
