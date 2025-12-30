package com.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    
    @Value("${rabbitmq.exchange.ticket}")
    private String ticketExchange;
    
    @Value("${rabbitmq.queue.ticket-created}")
    private String ticketCreatedQueue;
    
    @Value("${rabbitmq.queue.ticket-events}")
    private String ticketEventsQueue;
    
    @Value("${rabbitmq.queue.comment-events}")
    private String commentEventsQueue;
    
    @Value("${rabbitmq.routing-key.ticket-created}")
    private String ticketCreatedRoutingKey;
    
    @Value("${rabbitmq.routing-key.ticket-updated}")
    private String ticketUpdatedRoutingKey;
    
    @Value("${rabbitmq.routing-key.comment-added}")
    private String commentAddedRoutingKey;
    
    // Exchange
    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(ticketExchange);
    }
    
    // Queues
    @Bean
    public Queue ticketCreatedQueue() {
        return new Queue(ticketCreatedQueue, true);
    }
    
    @Bean
    public Queue ticketEventsQueue() {
        return new Queue(ticketEventsQueue, true);
    }
    
    @Bean
    public Queue commentEventsQueue() {
        return new Queue(commentEventsQueue, true);
    }
    
    // Bindings
    @Bean
    public Binding ticketCreatedBinding() {
        return BindingBuilder
                .bind(ticketCreatedQueue())
                .to(ticketExchange())
                .with(ticketCreatedRoutingKey);
    }
    
    @Bean
    public Binding ticketUpdatedBinding() {
        return BindingBuilder
                .bind(ticketEventsQueue())
                .to(ticketExchange())
                .with(ticketUpdatedRoutingKey);
    }
    
    @Bean
    public Binding commentAddedBinding() {
        return BindingBuilder
                .bind(commentEventsQueue())
                .to(ticketExchange())
                .with(commentAddedRoutingKey);
    }
    
    // Message Converter with Java 8 Time support
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    
    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
