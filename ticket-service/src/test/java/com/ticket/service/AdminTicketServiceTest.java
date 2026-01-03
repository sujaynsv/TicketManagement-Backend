package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketPriority;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketStatusChangedEvent;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminTicketServiceTest {

    @InjectMocks
    private AdminTicketService adminTicketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketActivityRepository ticketActivityRepository;

    @Mock
    private EventPublisherService eventPublisher;

    @Mock
    private MongoTemplate mongoTemplate;

    private Ticket testTicket;
    private LocalDateTime now;
    private String adminId = "admin-001";
    private String adminUsername = "admin";

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
        testTicket.setCategory(TicketCategory.TECHNICAL_ISSUE);
        testTicket.setPriority(TicketPriority.MEDIUM);
        testTicket.setCreatedByUserId("user1");
        testTicket.setCreatedByUsername("testuser");
        testTicket.setTags(List.of("tag1", "tag2"));
        testTicket.setCommentCount(0);
        testTicket.setAttachmentCount(0);
        testTicket.setCreatedAt(now);
        testTicket.setUpdatedAt(now);
    }

    // ==================== GET ALL TICKETS TESTS ====================

    @Test
    void testGetAllTickets_WithNoFilters_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Ticket", result.getContent().get(0).title());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Ticket.class));
    }

    @Test
    void testGetAllTickets_WithStatusFilter_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, "OPEN", null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Ticket.class));
    }

    @Test
    void testGetAllTickets_WithPriorityFilter_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, null, "MEDIUM", null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllTickets_WithCategoryFilter_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, null, null, "TECHNICAL_ISSUE", null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllTickets_WithSearchFilter_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, null, null, null, null, null, "Test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetAllTickets_WithInvalidStatus_IgnoresFilter() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, "INVALID_STATUS", null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== GET TICKET BY ID TESTS ====================

    @Test
    void testGetTicketById_WithValidId_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));

        // Act
        AdminTicketDTO result = adminTicketService.getTicketById("TKT-001");

        // Assert
        assertNotNull(result);
        assertEquals("Test Ticket", result.title());
        assertEquals("TKT-001", result.ticketId());
        verify(ticketRepository, times(1)).findById("TKT-001");
    }

    @Test
    void testGetTicketById_WithInvalidId_ThrowsException() {
        // Arrange
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adminTicketService.getTicketById("INVALID"));
    }

    // ==================== CHANGE PRIORITY TESTS ====================

    @Test
    void testChangePriority_FromMediumToHigh_Success() {
        // Arrange
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("HIGH", "Urgent issue");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        AdminTicketDTO result = adminTicketService.changePriority(
                "TKT-001", request, adminId, adminUsername);

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
    }

    @Test
    void testChangePriority_FromNullToHigh_Success() {
        // Arrange
        testTicket.setPriority(null);
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("HIGH", "Setting priority");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        AdminTicketDTO result = adminTicketService.changePriority(
                "TKT-001", request, adminId, adminUsername);

        // Assert
        assertNotNull(result);
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
    }

    @Test
    void testChangePriority_WithInvalidId_ThrowsException() {
        // Arrange
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("HIGH", "Reason");
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            adminTicketService.changePriority("INVALID", request, adminId, adminUsername));
    }

    // ==================== CHANGE CATEGORY TESTS ====================

    @Test
    void testChangeCategory_Success() {
        // Arrange
        AdminChangeCategoryRequest request = new AdminChangeCategoryRequest(
                "FEATURE_REQUEST", "Reclassifying ticket");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        AdminTicketDTO result = adminTicketService.changeCategory(
                "TKT-001", request, adminId, adminUsername);

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).findById("TKT-001");
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
    }

    @Test
    void testChangeCategory_WithInvalidId_ThrowsException() {
        // Arrange
        AdminChangeCategoryRequest request = new AdminChangeCategoryRequest("BUG_REPORT", "Reason");
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            adminTicketService.changeCategory("INVALID", request, adminId, adminUsername));
    }

    // ==================== CHANGE STATUS TESTS ====================

    @Test
    void testChangeStatus_ForceChangeFromOpenToClosed_Success() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Closing ticket");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        AdminTicketDTO result = adminTicketService.changeStatus(
                "TKT-001", request, adminId, adminUsername);

        // Assert
        assertNotNull(result);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(eventPublisher, times(1)).publishTicketStatusChanged(any(TicketStatusChangedEvent.class));
    }

    @Test
    void testChangeStatus_ToAssigned_SetsAssignedAt() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("ASSIGNED", "Assigning");
        testTicket.setAssignedAt(null);
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        adminTicketService.changeStatus("TKT-001", request, adminId, adminUsername);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getAssignedAt());
    }

    @Test
    void testChangeStatus_ToResolved_SetsResolvedAt() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("RESOLVED", "Resolving");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        adminTicketService.changeStatus("TKT-001", request, adminId, adminUsername);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getResolvedAt());
    }

    @Test
    void testChangeStatus_ToClosed_SetsClosedAt() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Closing");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        adminTicketService.changeStatus("TKT-001", request, adminId, adminUsername);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getClosedAt());
    }

    @Test
    void testChangeStatus_WithInvalidId_ThrowsException() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Reason");
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            adminTicketService.changeStatus("INVALID", request, adminId, adminUsername));
    }

    // ==================== DELETE TICKET TESTS ====================

    @Test
    void testDeleteTicket_HardDelete_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));

        // Act
        String result = adminTicketService.deleteTicket("TKT-001", true, adminId, adminUsername);

        // Assert
        assertEquals("Ticket permanently deleted", result);
        verify(ticketRepository, times(1)).delete(testTicket);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testDeleteTicket_SoftDelete_Success() {
        // Arrange
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        String result = adminTicketService.deleteTicket("TKT-001", false, adminId, adminUsername);

        // Assert
        assertEquals("Ticket closed (soft delete)", result);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(ticketActivityRepository, times(1)).save(any(TicketActivity.class));
        verify(ticketRepository, never()).delete(any(Ticket.class));
    }

    @Test
    void testDeleteTicket_WithInvalidId_ThrowsException() {
        // Arrange
        when(ticketRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            adminTicketService.deleteTicket("INVALID", true, adminId, adminUsername));
    }

    // ==================== GET TICKET STATS TESTS ====================

    @Test
    void testGetTicketStats_Success() {
        // Arrange
        when(ticketRepository.count()).thenReturn(100L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.ASSIGNED)).thenReturn(15L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(30L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.ESCALATED)).thenReturn(5L);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(10L);

        // Act
        TicketStatsDTO result = adminTicketService.getTicketStats();

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.totalTickets());
        assertEquals(20L, result.openTickets());
        assertEquals(15L, result.assignedTickets());
        assertEquals(10L, result.inProgressTickets());
        assertEquals(30L, result.resolvedTickets());
        assertEquals(20L, result.closedTickets());
        assertEquals(5L, result.escalatedTickets());
        verify(ticketRepository, times(1)).count();
    }

    // ==================== GET USER TICKETS TESTS ====================

    @Test
    void testGetUserTickets_Success() {
        // Arrange
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getUserTickets("user1", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Ticket.class));
    }

    // ==================== GET AGENT TICKETS TESTS ====================

    @Test
    void testGetAgentTickets_Success() {
        // Arrange
        testTicket.setAssignedToUserId("agent1");
        List<Ticket> tickets = List.of(testTicket);
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(tickets);
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAgentTickets("agent1", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Ticket.class));
    }

    // ==================== EDGE CASES ====================

    @Test
    void testGetAllTickets_WithEmptyResult_ReturnsEmptyPage() {
        // Arrange
        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(0L);

        // Act
        Page<AdminTicketDTO> result = adminTicketService.getAllTickets(
                0, 10, null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testChangePriority_UpdatesTimestamp() {
        // Arrange
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("CRITICAL", "Urgent");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        adminTicketService.changePriority("TKT-001", request, adminId, adminUsername);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getUpdatedAt());
    }

    @Test
    void testChangeCategory_UpdatesTimestamp() {
        // Arrange
        AdminChangeCategoryRequest request = new AdminChangeCategoryRequest("BILLING", "Reclassify");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        // Act
        adminTicketService.changeCategory("TKT-001", request, adminId, adminUsername);

        // Assert
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertNotNull(savedTicket.getUpdatedAt());
    }

    @Test
    void testChangeStatus_PublishesEventWithCorrectData() {
        // Arrange
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Done");
        when(ticketRepository.findById("TKT-001")).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        ArgumentCaptor<TicketStatusChangedEvent> eventCaptor = 
                ArgumentCaptor.forClass(TicketStatusChangedEvent.class);

        // Act
        adminTicketService.changeStatus("TKT-001", request, adminId, adminUsername);

        // Assert
        verify(eventPublisher).publishTicketStatusChanged(eventCaptor.capture());
        TicketStatusChangedEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals("TKT-001", event.getTicketId());
        assertEquals(adminId, event.getChangedByUserId());
    }
}
