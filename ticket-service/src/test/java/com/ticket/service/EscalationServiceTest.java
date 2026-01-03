package com.ticket.service;

import com.ticket.client.AssignmentServiceClient;
import com.ticket.client.UserServiceClient;
import com.ticket.dto.AssignmentDTO;
import com.ticket.dto.EscalateTicketRequest;
import com.ticket.dto.UserDTO;
import com.ticket.entity.Ticket;
import com.ticket.enums.EscalationType;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketPriority;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @InjectMocks
    private EscalationService escalationService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AssignmentServiceClient assignmentServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private EventPublisherService eventPublisherService;

    private Ticket testTicket;
    @Mock
    private AssignmentDTO testAssignment;
    private UserDTO testManager;
    private EscalateTicketRequest escalateRequest;
    private LocalDateTime now;
    private String ticketId = "TKT-001";
    private String escalatedBy = "agent-001";
    private String escalatedByUsername = "agent";

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Setup test ticket
        testTicket = new Ticket();
        testTicket.setTicketId(ticketId);
        testTicket.setTicketNumber("TKT-20240101-00001");
        testTicket.setTitle("Test Ticket");
        testTicket.setStatus(TicketStatus.ASSIGNED);
        testTicket.setCategory(TicketCategory.TECHNICAL_ISSUE);
        testTicket.setPriority(TicketPriority.HIGH);
        testTicket.setAssignedToUserId("agent-001");
        testTicket.setAssignedToUsername("agent");
        testTicket.setCreatedAt(now);
        testTicket.setUpdatedAt(now);

        // Setup test assignment - lenient stubbing to avoid unnecessary stubbing errors
        lenient().when(testAssignment.getAssignedBy()).thenReturn("manager-001");
        lenient().when(testAssignment.getAssignedByUsername()).thenReturn("manager");

        // Setup test manager
        testManager = new UserDTO();
        testManager.setUserId("manager-001");
        testManager.setUsername("manager");
        testManager.setEmail("manager@test.com");
        testManager.setRole("MANAGER");

        // Setup escalate request
        escalateRequest = new EscalateTicketRequest();
        escalateRequest.setReason("Issue requires manager attention");
    }

    // ==================== SUCCESSFUL ESCALATION TESTS ====================

    @Test
    void testEscalateTicket_WithValidRequest_Success() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        Ticket result = escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(assignmentServiceClient, times(1)).getAssignmentByTicketId(ticketId);
        verify(userServiceClient, times(1)).getUserById("manager-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(eventPublisherService, times(1)).publishTicketEscalated(any(TicketEscalatedEvent.class));
    }

    @Test
    void testEscalateTicket_UpdatesTicketFields() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(testTicket);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(TicketStatus.ESCALATED, savedTicket.getStatus());
        assertEquals("manager-001", savedTicket.getEscalatedToUserId());
        assertEquals("manager", savedTicket.getEscalatedToUsername());
        assertEquals(escalatedBy, savedTicket.getEscalatedBy());
        assertEquals(escalatedByUsername, savedTicket.getEscalatedByUsername());
        assertEquals(EscalationType.MANUAL, savedTicket.getEscalationType());
        assertEquals("Issue requires manager attention", savedTicket.getEscalationReason());
        assertNotNull(savedTicket.getEscalatedAt());
        assertNotNull(savedTicket.getUpdatedAt());
    }

    @Test
    void testEscalateTicket_ReassignsToManager() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(testTicket);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.AUTO_SLA_BREACH);

        // Assert
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals("manager-001", savedTicket.getAssignedToUserId());
        assertEquals("manager", savedTicket.getAssignedToUsername());
    }

    @Test
    void testEscalateTicket_PublishesEvent() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ArgumentCaptor<TicketEscalatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketEscalatedEvent.class);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        verify(eventPublisherService).publishTicketEscalated(eventCaptor.capture());
        TicketEscalatedEvent event = eventCaptor.getValue();
        assertEquals(ticketId, event.getTicketId());
        assertEquals("TKT-20240101-00001", event.getTicketNumber());
        assertEquals("Test Ticket", event.getTitle());
        assertEquals("TECHNICAL_ISSUE", event.getCategory());
        assertEquals("HIGH", event.getPriority());
        assertEquals("MANUAL", event.getEscalationType());
        assertEquals("Issue requires manager attention", event.getEscalationReason());
        assertEquals(escalatedBy, event.getEscalatedBy());
        assertEquals(escalatedByUsername, event.getEscalatedByUsername());
        assertEquals("manager-001", event.getEscalatedToUserId());
        assertEquals("manager", event.getEscalatedToUsername());
        assertEquals("manager@test.com", event.getEscalatedToEmail());
        assertEquals("agent-001", event.getPreviousAgentId());
        assertEquals("agent", event.getPreviousAgentUsername());
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    void testEscalateTicket_WithInvalidTicketId_ThrowsException() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertTrue(exception.getMessage().contains("Ticket not found"));
        verify(assignmentServiceClient, never()).getAssignmentByTicketId(anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testEscalateTicket_WithAlreadyEscalatedTicket_ThrowsException() {
        // Arrange
        testTicket.setStatus(TicketStatus.ESCALATED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertEquals("Ticket is already escalated", exception.getMessage());
        verify(assignmentServiceClient, never()).getAssignmentByTicketId(anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testEscalateTicket_WithClosedTicket_ThrowsException() {
        // Arrange
        testTicket.setStatus(TicketStatus.CLOSED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertEquals("Cannot escalate a closed ticket", exception.getMessage());
        verify(assignmentServiceClient, never()).getAssignmentByTicketId(anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testEscalateTicket_WithNoAssignment_ThrowsException() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertTrue(exception.getMessage().contains("Ticket is not assigned"));
        verify(userServiceClient, never()).getUserById(anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testEscalateTicket_WithAssignmentServiceFailure_ThrowsException() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId))
                .thenThrow(new RuntimeException("Assignment service down"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertTrue(exception.getMessage().contains("Ticket is not assigned"));
        verify(userServiceClient, never()).getUserById(anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testEscalateTicket_WithUserServiceFailure_ThrowsException() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001"))
                .thenThrow(new RuntimeException("User service down"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            escalationService.escalateTicket(
                    ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL));
        assertTrue(exception.getMessage().contains("Failed to fetch manager details"));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ==================== ESCALATION TYPE TESTS ====================

    @Test
    void testEscalateTicket_WithManualEscalation_Success() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(testTicket);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(EscalationType.MANUAL, savedTicket.getEscalationType());
    }

    @Test
    void testEscalateTicket_WithAutoSlaBreachEscalation_Success() {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(testTicket);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.AUTO_SLA_BREACH);

        // Assert
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(EscalationType.AUTO_SLA_BREACH, savedTicket.getEscalationType());
    }

    // ==================== EDGE CASES ====================

    @Test
    void testEscalateTicket_WithNullCategoryAndPriority_Success() {
        // Arrange
        testTicket.setCategory(null);
        testTicket.setPriority(null);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ArgumentCaptor<TicketEscalatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketEscalatedEvent.class);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        verify(eventPublisherService).publishTicketEscalated(eventCaptor.capture());
        TicketEscalatedEvent event = eventCaptor.getValue();
        assertNull(event.getCategory());
        assertNull(event.getPriority());
    }

    @Test
    void testEscalateTicket_PreservesPreviousAgentInfo() {
        // Arrange
        testTicket.setAssignedToUserId("original-agent-id");
        testTicket.setAssignedToUsername("original-agent");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(assignmentServiceClient.getAssignmentByTicketId(ticketId)).thenReturn(testAssignment);
        when(userServiceClient.getUserById("manager-001")).thenReturn(testManager);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ArgumentCaptor<TicketEscalatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketEscalatedEvent.class);

        // Act
        escalationService.escalateTicket(
                ticketId, escalatedBy, escalatedByUsername, escalateRequest, EscalationType.MANUAL);

        // Assert
        verify(eventPublisherService).publishTicketEscalated(eventCaptor.capture());
        TicketEscalatedEvent event = eventCaptor.getValue();
        assertEquals("original-agent-id", event.getPreviousAgentId());
        assertEquals("original-agent", event.getPreviousAgentUsername());
    }
}
