package com.assignment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
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
    
    public static final String EXCHANGE = "ticket.exchange";
    
    // Separate queues for each event type
    public static final String TICKET_CREATED_QUEUE = "assignment.ticket.created";
    public static final String TICKET_ASSIGNED_QUEUE = "assignment.ticket.assigned";
    public static final String TICKET_STATUS_QUEUE = "assignment.ticket.status";
    
    // Routing Keys
    public static final String TICKET_CREATED_KEY = "ticket.created";
    public static final String TICKET_ASSIGNED_KEY = "ticket.assigned";
    public static final String TICKET_STATUS_CHANGED_KEY = "ticket.status.changed";
    
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
    
    @Bean
    public Queue ticketCreatedQueue() {
        return QueueBuilder.durable(TICKET_CREATED_QUEUE).build();
    }
    
    @Bean
    public Queue ticketAssignedQueue() {
        return QueueBuilder.durable(TICKET_ASSIGNED_QUEUE).build();
    }
    
    @Bean
    public Queue ticketStatusQueue() {
        return QueueBuilder.durable(TICKET_STATUS_QUEUE).build();
    }
    
    @Bean
    public Binding ticketCreatedBinding(Queue ticketCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ticketCreatedQueue)
                .to(exchange)
                .with(TICKET_CREATED_KEY);
    }
    
    @Bean
    public Binding ticketAssignedBinding(Queue ticketAssignedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ticketAssignedQueue)
                .to(exchange)
                .with(TICKET_ASSIGNED_KEY);
    }
    
    @Bean
    public Binding ticketStatusChangedBinding(Queue ticketStatusQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ticketStatusQueue)
                .to(exchange)
                .with(TICKET_STATUS_CHANGED_KEY);
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
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}
