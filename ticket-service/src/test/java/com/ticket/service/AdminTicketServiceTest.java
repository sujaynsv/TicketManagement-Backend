package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketStatusChangedEvent;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketActivityRepository ticketActivityRepository;
    @Mock private EventPublisherService eventPublisher;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private AdminTicketService adminTicketService;

    private static TicketCategory anyCategory() {
        return TicketCategory.values()[0];
    }

    private Ticket ticketWithIdAndNumber(String id, String number) {
        Ticket t = mock(Ticket.class);
        when(t.getTicketId()).thenReturn(id);
        when(t.getTicketNumber()).thenReturn(number);
        // Everything else can be null; convertToAdminDTO handles nulls.
        return t;
    }

    @Test
    void getAllTickets_noFilters_returnsPage() {
        // NOTE: If your record signature differs, adjust args to match your TicketFilterRequest.
        TicketFilterRequest filter = new TicketFilterRequest(
                0, 10,
                null, null, null,
                null, null, null
        );

        Ticket t1 = ticketWithIdAndNumber("t1", "T-1");
        Ticket t2 = ticketWithIdAndNumber("t2", "T-2");

        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(List.of(t1, t2));
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(2L);

        Page<AdminTicketDTO> page = adminTicketService.getAllTickets(filter);

        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals("t1", page.getContent().get(0).ticketId());
        assertEquals("t2", page.getContent().get(1).ticketId());

        verify(mongoTemplate).find(any(Query.class), eq(Ticket.class));
        verify(mongoTemplate).count(any(Query.class), eq(Ticket.class));
    }

    @Test
    void getAllTickets_invalidEnums_doesNotThrow() {
        TicketFilterRequest filter = new TicketFilterRequest(
                0, 10,
                "NOT_A_STATUS", "NOT_A_PRIORITY", "NOT_A_CATEGORY",
                null, null, "abc"
        );

        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(0L);

        assertDoesNotThrow(() -> adminTicketService.getAllTickets(filter));

        verify(mongoTemplate).find(any(Query.class), eq(Ticket.class));
        verify(mongoTemplate).count(any(Query.class), eq(Ticket.class));
    }

    @Test
    void getTicketById_found_returnsDto() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");
        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));

        AdminTicketDTO dto = adminTicketService.getTicketById("t1");

        assertEquals("t1", dto.ticketId());
        assertEquals("T-1", dto.ticketNumber());
        verify(ticketRepository).findById("t1");
    }

    @Test
    void getTicketById_notFound_throws() {
        when(ticketRepository.findById("t404")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminTicketService.getTicketById("t404"));

        assertEquals("Ticket Not Found!", ex.getMessage());
    }

    @Test
    void changePriority_oldNull_logsActivity_andSaves() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");
        when(t.getPriority()).thenReturn(null);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));
        when(ticketRepository.save(t)).thenReturn(t);

        AdminChangePriorityRequest req = new AdminChangePriorityRequest("HIGH", "urgent");

        AdminTicketDTO dto = adminTicketService.changePriority("t1", req, "admin1", "adminUser");

        assertEquals("t1", dto.ticketId());
        verify(ticketRepository).save(t);
        verify(ticketActivityRepository).save(any(TicketActivity.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void changeCategory_logsActivity_andSaves() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");
        when(t.getCategory()).thenReturn(anyCategory());

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));
        when(ticketRepository.save(t)).thenReturn(t);

        String newCategory = anyCategory().name();
        AdminChangeCategoryRequest req = new AdminChangeCategoryRequest(newCategory, "reclassify");

        AdminTicketDTO dto = adminTicketService.changeCategory("t1", req, "admin1", "adminUser");

        assertEquals("t1", dto.ticketId());
        verify(ticketRepository).save(t);
        verify(ticketActivityRepository).save(any(TicketActivity.class));
    }

    @Test
    void changeStatus_assigned_setsAssignedAt_publishesEvent_logsActivity() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");
        when(t.getStatus()).thenReturn(TicketStatus.OPEN);
        when(t.getAssignedAt()).thenReturn(null);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));
        when(ticketRepository.save(t)).thenReturn(t);

        AdminChangeStatusRequest req = new AdminChangeStatusRequest("ASSIGNED", "force assign");

        AdminTicketDTO dto = adminTicketService.changeStatus("t1", req, "admin1", "adminUser");

        assertEquals("t1", dto.ticketId());

        verify(t).setStatus(TicketStatus.ASSIGNED);
        verify(t, atLeastOnce()).setUpdatedAt(any());
        verify(t).setAssignedAt(any());

        verify(ticketActivityRepository).save(any(TicketActivity.class));

        ArgumentCaptor<TicketStatusChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(TicketStatusChangedEvent.class);
        verify(eventPublisher).publishTicketStatusChanged(eventCaptor.capture());
    }

    @Test
    void deleteTicket_hardDelete_deletesAndReturnsMessage() {
        // Minimal stubs only (prevents UnnecessaryStubbingException) [web:73]
        Ticket t = mock(Ticket.class);
        when(t.getTicketNumber()).thenReturn("T-1");

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));

        String msg = adminTicketService.deleteTicket("t1", true, "admin1", "adminUser");

        assertEquals("Ticket permanently deleted", msg);
        verify(ticketRepository).delete(t);
        verify(ticketActivityRepository, never()).save(any());
    }

    @Test
    void deleteTicket_softDelete_savesActivity_andReturnsMessage() {
        // Minimal stubs only (prevents UnnecessaryStubbingException) [web:73]
        Ticket t = mock(Ticket.class);
        when(t.getTicketNumber()).thenReturn("T-1");

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(t));
        when(ticketRepository.save(t)).thenReturn(t);

        String msg = adminTicketService.deleteTicket("t1", false, "admin1", "adminUser");

        assertEquals("Ticket closed (soft delete)", msg);

        verify(t).setStatus(TicketStatus.CLOSED);
        verify(t).setClosedAt(any());
        verify(t, atLeastOnce()).setUpdatedAt(any());

        verify(ticketRepository).save(t);
        verify(ticketActivityRepository).save(any(TicketActivity.class));
    }

    @Test
    void getTicketStats_returnsAggregatedCounts() {
        when(ticketRepository.count()).thenReturn(100L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.ASSIGNED)).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(30L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(15L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.ESCALATED)).thenReturn(5L);

        // countTicketsByPriority() is called 5 times: CRITICAL, HIGH, MEDIUM, LOW, null
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class)))
                .thenReturn(1L, 2L, 3L, 4L, 5L);

        TicketStatsDTO stats = adminTicketService.getTicketStats();

        assertEquals(100L, stats.totalTickets());
        assertEquals(10L, stats.openTickets());
        assertEquals(1L, stats.criticalTickets());
        assertEquals(5L, stats.noPriorityTickets());

        verify(mongoTemplate, times(5)).count(any(Query.class), eq(Ticket.class));
    }

    @Test
    void getUserTickets_returnsPage() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");

        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(List.of(t));
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        Page<AdminTicketDTO> page = adminTicketService.getUserTickets("u1", 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
        assertEquals("t1", page.getContent().get(0).ticketId());
    }

    @Test
    void getAgentTickets_returnsPage() {
        Ticket t = ticketWithIdAndNumber("t1", "T-1");

        when(mongoTemplate.find(any(Query.class), eq(Ticket.class))).thenReturn(List.of(t));
        when(mongoTemplate.count(any(Query.class), eq(Ticket.class))).thenReturn(1L);

        Page<AdminTicketDTO> page = adminTicketService.getAgentTickets("agent1", 0, 10);

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
        assertEquals("t1", page.getContent().get(0).ticketId());
    }
}
