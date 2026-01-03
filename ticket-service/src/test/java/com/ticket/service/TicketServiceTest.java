package com.ticket.service;

import com.ticket.dto.ChangeStatusRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.entity.Comment;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketPriority;
import com.ticket.enums.TicketStatus;
import com.ticket.event.CommentAddedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import com.ticket.repository.CommentRepository;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @InjectMocks
    private TicketService ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketActivityRepository ticketActivityRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @Mock
    private CommentRepository commentRepository;

    private Ticket testTicket;
    private CreateTicketRequest createRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        // Setup test ticket
        testTicket = new Ticket();
        testTicket.setTicketId("TKT-001");
        testTicket.setTicketNumber("TKT-20240101-00001");
        testTicket.setTitle("Test Ticket");
        testTicket.setDescription("Test Description");
        testTicket.setStatus(TicketStatus.OPEN);
        testTicket.setCategory(TicketCategory.BUG);
        testTicket.setPriority(TicketPriority.HIGH);
        testTicket.setCreatedByUserId("user1");
        testTicket.setCreatedByUsername("testuser");
        testTicket.setTags(List.of("tag1", "tag2"));
        testTicket.setCommentCount(0);
        testTicket.setAttachmentCount(0);
        testTicket.setCreatedAt(now);
        testTicket.setUpdatedAt(now);

        // Setup create request
        createRequest = new CreateTicketRequest();
        createRequest.setTitle("New Ticket");
        createRequest.setDescription("New Description");
        createRequest.setCategory("BUG");
        createRequest.setTags(List.of("urgent"));
    }

    // ==================== CREATE TICKET TESTS ====================

    @Test
    void testCreateTicket_WithValidRequest_Success() {
        // Arrange
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketRepository.count()).thenReturn(0L);

        // Act
        TicketDTO result = ticketService.createTicket(createRequest, "user1", "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("Test Ticket", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals("OPEN", result.getStatus());
        assertEquals("BUG", result.getCategory());
        verify(ticketRepository, times(2)).save(any(Ticket.class)); // Once for save, once for increment counts
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(eventPublisher, times(1)).publishTicketCreated(any(TicketCreatedEvent.class));
    }

    @Test
    void testCreateTicket_WithNullTags_DefaultsToEmptyList() {
        // Arrange
        createRequest.setTags(null);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketRepository.count()).thenReturn(0L);

        // Act
        TicketDTO result = ticketService.createTicket(createRequest, "user1", "testuser");

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(2)).save(any(Ticket.class));
    }

    @Test
    void testCreateTicket_PublishesEvent() {
        // Arrange
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(ticketRepository.count()).thenReturn(0L);
        ArgumentCaptor<TicketCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TicketCreatedEvent.class);

        // Act
        ticketService.createTicket(createRequest, "user1", "testuser");

        // Assert
        verify(eventPublisher).publishTicketCreated(eventCaptor.capture());
        TicketCreatedEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals("TKT-001", event.getTicketId());
    }

    // ==================== GET TICKET TESTS ====================

    @Test
    void testGetTicketById_WithValidId_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));

        // Act
        TicketDTO result = ticketService.getTicketById("TKT-001");

        // Assert
        assertNotNull(result);
        assertEquals("Test Ticket", result.getTitle());
        assertEquals("TKT-001", result.getTicketId());
        verify(ticketRepository, times(1)).findById("TKT-001");
    }

    @Test
    void testGetTicketById_WithInvalidId_ThrowsException() {
        // Arrange
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> ticketService.getTicketById("INVALID"));
        verify(ticketRepository, times(1)).findById("INVALID");
    }

    @Test
    void testGetTicketByNumber_WithValidNumber_Success() {
        // Arrange
        when(ticketRepository.findByTicketNumber("TKT-20240101-00001")).thenReturn(Optional.of(testTicket));

        // Act
        TicketDTO result = ticketService.getTicketByNumber("TKT-20240101-00001");

        // Assert
        assertNotNull(result);
        assertEquals("Test Ticket", result.getTitle());
        verify(ticketRepository, times(1)).findByTicketNumber("TKT-20240101-00001");
    }

    @Test
    void testGetTicketByNumber_WithInvalidNumber_ThrowsException() {
        // Arrange
        when(ticketRepository.findByTicketNumber("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> ticketService.getTicketByNumber("INVALID"));
    }

    // ==================== UPDATE TICKET TESTS ====================

    @Test
    void testUpdateTicket_WithValidRequest_Success() {
        // Arrange
        UpdateTicketRequest updateRequest = new UpdateTicketRequest("Updated Title", "Updated Desc", "FEATURE", "MEDIUM", List.of("new-tag"));
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        TicketDTO result = ticketService.updateTicket("TKT-001", updateRequest, "user2", "updater");

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
    }

    @Test
    void testUpdateTicket_WithPartialUpdate_KeepsExistingValues() {
        // Arrange
        UpdateTicketRequest updateRequest = new UpdateTicketRequest("Updated Title", null, null, null, null);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.updateTicket("TKT-001", updateRequest, "user2", "updater");

        // Assert
        verify(ticketRepository, times(1)).save(argThat(t -> t.getTitle().equals("Updated Title")));
    }

    @Test
    void testUpdateTicket_WithInvalidId_ThrowsException() {
        // Arrange
        UpdateTicketRequest updateRequest = new UpdateTicketRequest("Updated Title", "Updated Desc", "FEATURE", "MEDIUM", null);
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> ticketService.updateTicket("INVALID", updateRequest, "user2", "updater"));
    }

    // ==================== CHANGE STATUS TESTS ====================

    @Test
    void testChangeStatus_FromOpenToAssigned_Success() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("ASSIGNED", null);
        testTicket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        TicketDTO result = ticketService.changeStatus("TKT-001", request, "user1", "testuser");

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(eventPublisher, times(1)).publishTicketStatusChanged(any(TicketStatusChangedEvent.class));
    }

    @Test
    void testChangeStatus_WithComment_CreatesComment() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("ASSIGNED", "Assigned to team");
        testTicket.setStatus(TicketStatus.OPEN);
        Comment savedComment = new Comment();
        savedComment.setCommentId("CMT-001");
        savedComment.setTicketId("TKT-001");
        savedComment.setCommentText("Assigned to team");
        
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        TicketDTO result = ticketService.changeStatus("TKT-001", request, "user1", "testuser");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        Comment savedCommentArg = commentCaptor.getValue();
        assertEquals("Assigned to team", savedCommentArg.getCommentText());
        verify(eventPublisher, times(1)).publishCommentAdded(any(CommentAddedEvent.class));
    }

    @Test
    void testChangeStatus_WithEmptyComment_DoesNotCreateComment() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("ASSIGNED", "   ");
        testTicket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.changeStatus("TKT-001", request, "user1", "testuser");

        // Assert
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void testChangeStatus_ToResolved_SetsResolvedAt() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("RESOLVED", null);
        testTicket.setStatus(TicketStatus.ASSIGNED);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.changeStatus("TKT-001", request, "user1", "testuser");

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, times(1)).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getResolvedAt());
    }

    @Test
    void testChangeStatus_ToClosed_SetsClosedAt() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("CLOSED", null);
        testTicket.setStatus(TicketStatus.RESOLVED);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.changeStatus("TKT-001", request, "user1", "testuser");

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, times(1)).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getClosedAt());
    }

    @Test
    void testChangeStatus_InvalidTransition_ThrowsException() {
        // Arrange
        ChangeStatusRequest request = new ChangeStatusRequest("OPEN", null);
        testTicket.setStatus(TicketStatus.CLOSED);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> ticketService.changeStatus("TKT-001", request, "user1", "testuser"));
    }

    // ==================== GET TICKETS BY FILTER TESTS ====================

    @Test
    void testGetMyTickets_ReturnsUserTickets() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(ticketRepository.findByCreatedByUserId("user1")).thenReturn(tickets);

        // Act
        List<TicketDTO> result = ticketService.getMyTickets("user1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketRepository, times(1)).findByCreatedByUserId("user1");
    }

    @Test
    void testGetAssignedTickets_ReturnsAssignedTickets() {
        // Arrange
        testTicket.setAssignedToUserId("user2");
        List<Ticket> tickets = List.of(testTicket);
        when(ticketRepository.findByAssignedToUserId("user2")).thenReturn(tickets);

        // Act
        List<TicketDTO> result = ticketService.getAssignedTickets("user2");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketRepository, times(1)).findByAssignedToUserId("user2");
    }

    @Test
    void testGetTicketsByStatus_ReturnsTicketsWithStatus() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(ticketRepository.findByStatus(TicketStatus.OPEN)).thenReturn(tickets);

        // Act
        List<TicketDTO> result = ticketService.getTicketsByStatus("OPEN");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketRepository, times(1)).findByStatus(TicketStatus.OPEN);
    }

    @Test
    void testGetAllTickets_ReturnsAllTickets() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(ticketRepository.findAll()).thenReturn(tickets);

        // Act
        List<TicketDTO> result = ticketService.getAllTickets();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(ticketRepository, times(1)).findAll();
    }

    // ==================== DELETE & COUNT TESTS ====================

    @Test
    void testDeleteTicket_Success() {
        // Act
        ticketService.deleteTicket("TKT-001");

        // Assert
        verify(ticketRepository, times(1)).deleteById("TKT-001");
    }

    @Test
    void testIncrementCommentCount_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.incrementCommentCount("TKT-001");

        // Assert
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testIncrementAttachmentCount_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.incrementAttachmentCount("TKT-001");

        // Assert
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void testUpdateAttachmentCount_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.updateAttachmentCount("TKT-001", 5);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertEquals(5, ticketCaptor.getValue().getAttachmentCount());
    }

    // ==================== UPDATE PRIORITY TESTS ====================

    @Test
    void testUpdateTicketPriority_WhenPriorityIsNull_SetsPriorityAndPublishesEvent() {
        // Arrange
        testTicket.setPriority(null);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.updateTicketPriority("TKT-001", "HIGH", "Urgent fix needed", "manager1", "manager");

        // Assert
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(eventPublisher, times(1)).publishTicketCreated(any(TicketCreatedEvent.class));
    }

    @Test
    void testUpdateTicketPriority_WhenPriorityExists_UpdatesWithoutEvent() {
        // Arrange
        testTicket.setPriority(TicketPriority.LOW);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.updateTicketPriority("TKT-001", "HIGH", "Escalated", "manager1", "manager");

        // Assert
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(eventPublisher, never()).publishTicketCreated(any(TicketCreatedEvent.class));
    }

    @Test
    void testUpdateTicketPriority_WithNullReason_UsesDefault() {
        // Arrange
        testTicket.setPriority(null);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        ticketService.updateTicketPriority("TKT-001", "HIGH", null, "manager1", "manager");

        // Assert
        ArgumentCaptor<TicketActivity> activityCaptor = ArgumentCaptor.forClass(TicketActivity.class);
        verify(ticketActivityRepository).save(activityCaptor.capture());
        TicketActivity activity = activityCaptor.getValue();
        assertTrue(activity.getDescription().contains("Not specified"));
    }

    @Test
    void testUpdateTicketPriority_WithInvalidId_ThrowsException() {
        // Arrange
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            ticketService.updateTicketPriority("INVALID", "HIGH", "Reason", "manager1", "manager")
        );
    }
}
