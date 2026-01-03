package com.ticket.service;

import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.event.TicketStatusChangedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherServiceTest {

    @InjectMocks
    private EventPublisherService eventPublisherService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private String ticketExchange = "ticket.exchange";
    private String ticketCreatedRoutingKey = "ticket.created";
    private String ticketStatusChangedRoutingKey = "ticket.status.changed";
    private String commentAddedRoutingKey = "comment.added";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisherService, "ticketExchange", ticketExchange);
        ReflectionTestUtils.setField(eventPublisherService, "ticketCreatedRoutingKey", ticketCreatedRoutingKey);
        ReflectionTestUtils.setField(eventPublisherService, "ticketStatusChangedRoutingKey", ticketStatusChangedRoutingKey);
        ReflectionTestUtils.setField(eventPublisherService, "commentAddedRoutingKey", commentAddedRoutingKey);
    }

    // ==================== TICKET CREATED EVENT TESTS ====================

    @Test
    void testPublishTicketCreated_Success() {
        // Arrange
        TicketCreatedEvent event = new TicketCreatedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "Test Ticket",
                "Description",
                "OPEN",
                "TECHNICAL_ISSUE",
                "user1",
                "testuser",
                LocalDateTime.now()
        );

        // Act
        eventPublisherService.publishTicketCreated(event);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                ticketExchange,
                ticketCreatedRoutingKey,
                event
        );
    }

    @Test
    void testPublishTicketCreated_WithCorrectRoutingKey() {
        // Arrange
        TicketCreatedEvent event = new TicketCreatedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "Test Ticket",
                "Description",
                "OPEN",
                "TECHNICAL_ISSUE",
                "user1",
                "testuser",
                LocalDateTime.now()
        );

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        eventPublisherService.publishTicketCreated(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                any(TicketCreatedEvent.class)
        );
        assertEquals(ticketExchange, exchangeCaptor.getValue());
        assertEquals(ticketCreatedRoutingKey, routingKeyCaptor.getValue());
    }

    // ==================== TICKET STATUS CHANGED EVENT TESTS ====================

    @Test
    void testPublishTicketStatusChanged_Success() {
        // Arrange
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "OPEN",
                "ASSIGNED",
                "user1",
                "testuser",
                "Status changed",
                LocalDateTime.now()
        );

        // Act
        eventPublisherService.publishTicketStatusChanged(event);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                ticketExchange,
                ticketStatusChangedRoutingKey,
                event
        );
    }

    @Test
    void testPublishTicketStatusChanged_WithCorrectRoutingKey() {
        // Arrange
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "OPEN",
                "CLOSED",
                "user1",
                "testuser",
                "Closing ticket",
                LocalDateTime.now()
        );

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        eventPublisherService.publishTicketStatusChanged(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(ticketExchange),
                routingKeyCaptor.capture(),
                any(TicketStatusChangedEvent.class)
        );
        assertEquals(ticketStatusChangedRoutingKey, routingKeyCaptor.getValue());
    }

    // ==================== COMMENT ADDED EVENT TESTS ====================

    @Test
    void testPublishCommentAdded_Success() {
        // Arrange
        CommentAddedEvent event = new CommentAddedEvent(
                "CMT-001",
                "TKT-001",
                "TKT-20240101-00001",
                "user1",
                "testuser",
                "Test comment",
                false,
                LocalDateTime.now()
        );

        // Act
        eventPublisherService.publishCommentAdded(event);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                ticketExchange,
                commentAddedRoutingKey,
                event
        );
    }

    @Test
    void testPublishCommentAdded_WithCorrectRoutingKey() {
        // Arrange
        CommentAddedEvent event = new CommentAddedEvent(
                "CMT-001",
                "TKT-001",
                "TKT-20240101-00001",
                "user2",
                "otheruser",
                "Another comment",
                true,
                LocalDateTime.now()
        );

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        eventPublisherService.publishCommentAdded(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(ticketExchange),
                routingKeyCaptor.capture(),
                any(CommentAddedEvent.class)
        );
        assertEquals(commentAddedRoutingKey, routingKeyCaptor.getValue());
    }

    // ==================== TICKET ESCALATED EVENT TESTS ====================

    @Test
    void testPublishTicketEscalated_Success() {
        // Arrange
        TicketEscalatedEvent event = new TicketEscalatedEvent();
        event.setTicketId("TKT-001");
        event.setTicketNumber("TKT-20240101-00001");
        event.setEscalatedBy("user1");
        event.setEscalatedToUserId("manager1");
        event.setEscalationType("TECHNICAL");
        event.setEscalationReason("High priority issue");
        event.setEscalatedAt(LocalDateTime.now());

        // Act
        eventPublisherService.publishTicketEscalated(event);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("ticket.exchange"),
                eq("ticket.escalated"),
                eq(event)
        );
    }

    @Test
    void testPublishTicketEscalated_WithException_DoesNotThrow() {
        // Arrange
        TicketEscalatedEvent event = new TicketEscalatedEvent();
        event.setTicketId("TKT-001");
        event.setTicketNumber("TKT-20240101-00001");
        
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate)
                .convertAndSend(eq("ticket.exchange"), eq("ticket.escalated"), any(TicketEscalatedEvent.class));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> eventPublisherService.publishTicketEscalated(event));
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("ticket.exchange"),
                eq("ticket.escalated"),
                any(TicketEscalatedEvent.class)
        );
    }

    // ==================== EXCEPTION HANDLING TESTS ====================

    @Test
    void testPublishTicketCreated_RabbitMQFailure_ThrowsException() {
        // Arrange
        TicketCreatedEvent event = new TicketCreatedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "Test",
                "Desc",
                "OPEN",
                "BUG",
                "user1",
                "testuser",
                LocalDateTime.now()
        );
        
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate)
                .convertAndSend(eq(ticketExchange), eq(ticketCreatedRoutingKey), any(TicketCreatedEvent.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            eventPublisherService.publishTicketCreated(event));
    }

    @Test
    void testPublishTicketStatusChanged_RabbitMQFailure_ThrowsException() {
        // Arrange
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                "TKT-001",
                "TKT-20240101-00001",
                "OPEN",
                "CLOSED",
                "user1",
                "testuser",
                "Done",
                LocalDateTime.now()
        );
        
        doThrow(new RuntimeException("Connection error"))
                .when(rabbitTemplate)
                .convertAndSend(eq(ticketExchange), eq(ticketStatusChangedRoutingKey), any(TicketStatusChangedEvent.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            eventPublisherService.publishTicketStatusChanged(event));
    }

    // ==================== MULTIPLE EVENTS TESTS ====================

    @Test
    void testPublishMultipleEvents_AllSucceed() {
        // Arrange
        TicketCreatedEvent createdEvent = new TicketCreatedEvent(
                "TKT-001", "TKT-20240101-00001", "Title", "Desc", 
                "OPEN", "BUG", "user1", "testuser", LocalDateTime.now()
        );
        
        TicketStatusChangedEvent statusEvent = new TicketStatusChangedEvent(
                "TKT-001", "TKT-20240101-00001", "OPEN", "ASSIGNED",
                "user1", "testuser", "Status", LocalDateTime.now()
        );
        
        CommentAddedEvent commentEvent = new CommentAddedEvent(
                "CMT-001", "TKT-001", "TKT-20240101-00001", "user1",
                "testuser", "Comment", false, LocalDateTime.now()
        );

        // Act
        eventPublisherService.publishTicketCreated(createdEvent);
        eventPublisherService.publishTicketStatusChanged(statusEvent);
        eventPublisherService.publishCommentAdded(commentEvent);

        // Assert
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

        @Test
        void testPublishEvent_VerifiesEventData() {
        // Arrange
        TicketCreatedEvent event = new TicketCreatedEvent(
                "TKT-999",
                "TKT-20240101-00999",
                "Important Ticket",
                "Critical Description",
                "admin",
                "Administrator",
                "CRITICAL",
                "HIGH",
                LocalDateTime.now()
        );

        ArgumentCaptor<TicketCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketCreatedEvent.class);

        // Act
        eventPublisherService.publishTicketCreated(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eventCaptor.capture()
        );
        
        TicketCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals("TKT-999", capturedEvent.getTicketId());
        assertEquals("TKT-20240101-00999", capturedEvent.getTicketNumber());
        assertEquals("Important Ticket", capturedEvent.getTitle());
        assertEquals("Critical Description", capturedEvent.getDescription());
        assertEquals("admin", capturedEvent.getCreatedByUserId());
        assertEquals("Administrator", capturedEvent.getCreatedByUsername());
        assertEquals("CRITICAL", capturedEvent.getCategory());
        assertEquals("HIGH", capturedEvent.getPriority());
        }


}
