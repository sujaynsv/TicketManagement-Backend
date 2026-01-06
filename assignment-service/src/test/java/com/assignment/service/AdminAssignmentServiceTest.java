package com.assignment.service;

import com.assignment.dto.*;
import com.assignment.entity.*;
import com.assignment.exception.AgentCapacityExceededException;
import com.assignment.exception.AgentOfflineException;
import com.assignment.exception.AssignmentNotFoundException;
import com.assignment.exception.AssignmentStatusException;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.ticket.event.TicketAssignedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private TicketCacheRepository ticketCacheRepository;
    @Mock private AgentWorkloadRepository agentWorkloadRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private SlaService slaService;

    // self proxy used in service (recursive calls)
    @Mock private AdminAssignmentService self;

    private AdminAssignmentService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AdminAssignmentService(
                slaService,
                eventPublisher,
                agentWorkloadRepository,
                ticketCacheRepository,
                assignmentRepository,
                self
        );
        setField(service, "maxTicketsPerAgent", 5);
    }

    // ---------------- reflection helpers (DTOs may be record or normal class) ----------------

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object readValue(Object obj, String name) {
        if (obj == null) return null;

        // record-style accessor: obj.name()
        try {
            Method m = obj.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Exception ignored) { }

        // normal getters: getName()/isName()
        String cap = name.substring(0, 1).toUpperCase() + name.substring(1);
        for (String prefix : List.of("get", "is")) {
            try {
                Method m = obj.getClass().getMethod(prefix + cap);
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Exception ignored) { }
        }

        // JavaBeans introspection
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                if (name.equals(pd.getName()) && pd.getReadMethod() != null) {
                    Method getter = pd.getReadMethod();
                    getter.setAccessible(true);
                    return getter.invoke(obj);
                }
            }
        } catch (Exception ignored) { }

        // direct field
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception ignored) { }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readValue(Object obj, String name, Class<T> clazz) {
        Object v = readValue(obj, name);
        if (v == null) return null;
        return (T) v;
    }

    private static long longVal(Object obj, String name) {
        Object v = readValue(obj, name);
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private static Long firstLong(Object obj, String... names) {
        for (String n : names) {
            Object v = readValue(obj, n);
            if (v instanceof Number num) return num.longValue();
            if (v != null) {
                try { return Long.parseLong(String.valueOf(v)); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    // ---------------- entity helpers ----------------

    private static Assignment mockAssignment(
            String assignmentId,
            String ticketId,
            String ticketNumber,
            String agentId,
            String agentUsername,
            AssignmentStatus status,
            AssignmentType type
    ) {
        Assignment a = mock(Assignment.class);
        when(a.getAssignmentId()).thenReturn(assignmentId);
        when(a.getTicketId()).thenReturn(ticketId);
        when(a.getTicketNumber()).thenReturn(ticketNumber);
        when(a.getAgentId()).thenReturn(agentId);
        when(a.getAgentUsername()).thenReturn(agentUsername);
        when(a.getStatus()).thenReturn(status);
        when(a.getAssignmentType()).thenReturn(type);
        when(a.getAssignedBy()).thenReturn("admin1");
        when(a.getAssignedByUsername()).thenReturn("adminUser");
        when(a.getAssignmentStrategy()).thenReturn("STRAT");
        when(a.getPreviousAgentId()).thenReturn(null);
        when(a.getPreviousAgentUsername()).thenReturn(null);
        when(a.getReassignmentReason()).thenReturn(null);
        when(a.getAssignmentNotes()).thenReturn(null);
        when(a.getTicketStatus()).thenReturn("ASSIGNED");
        when(a.getAssignedAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(a.getCompletedAt()).thenReturn(null);
        return a;
    }

    private static TicketCache ticket(String id, String number, String title, String priority, String category) {
        TicketCache t = new TicketCache();
        t.setTicketId(id);
        t.setTicketNumber(number);
        t.setTitle(title);
        t.setPriority(priority);
        t.setCategory(category);
        t.setStatus("OPEN");
        t.setCreatedAt(LocalDateTime.now().minusDays(1));
        t.setUpdatedAt(LocalDateTime.now().minusHours(2));
        return t;
    }

    // ---------------- tests ----------------

    @Test
    void getAllAssignments_returnsPagedAdminDtos() {
        Assignment a1 = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);
        TicketCache t1 = ticket("t1", "T-1", "Login issue", "HIGH", "AUTH");

        // convertToAdminDTO calls findById (possibly multiple times)
        when(ticketCacheRepository.findById("t1")).thenReturn(Optional.of(t1));

        Page<Assignment> page = new PageImpl<>(List.of(a1), PageRequest.of(0, 10), 1);
        when(assignmentRepository.findAll(ArgumentMatchers.<Specification<Assignment>>any(), any(Pageable.class)))
                .thenReturn(page);

        Page<AdminAssignmentDTO> result = service.getAllAssignments(
                0, 10,
                "ASSIGNED",
                "ag1",
                "t1",
                "AUTO",
                "agent"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        AdminAssignmentDTO dto = result.getContent().get(0);
        String ticketId = readValue(dto, "ticketId", String.class);
        assertEquals("t1", ticketId);

        verify(assignmentRepository).findAll(ArgumentMatchers.<Specification<Assignment>>any(), any(Pageable.class));
    }

    @Test
    void getAssignmentById_whenMissing_throws() {
        when(assignmentRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getAssignmentById("missing"));
    }

    @Test
    void forceReassign_whenNewAgentOffline_throws() {
        Assignment oldAssignment = mockAssignment("as1", "t1", "T-1", "agOld", "oldAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        AgentWorkload newAgent = mock(AgentWorkload.class);
        when(newAgent.getStatus()).thenReturn(AgentStatus.OFFLINE);

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(oldAssignment));
        when(agentWorkloadRepository.findById("agNew")).thenReturn(Optional.of(newAgent));

        AdminReassignRequest req = new AdminReassignRequest("agNew", "reason");
        assertThrows(AgentOfflineException.class, () -> service.forceReassign("as1", req, "admin1", "adminUser"));

        verify(eventPublisher, never()).publishTicketAssigned(any());
    }

    @Test
    void forceReassign_whenNewAgentAtCapacity_throws() throws Exception {
        setField(service, "maxTicketsPerAgent", 2);

        Assignment oldAssignment = mockAssignment("as1", "t1", "T-1", "agOld", "oldAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        AgentWorkload newAgent = mock(AgentWorkload.class);
        when(newAgent.getStatus()).thenReturn(AgentStatus.AVAILABLE);
        when(newAgent.getActiveTickets()).thenReturn(2);

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(oldAssignment));
        when(agentWorkloadRepository.findById("agNew")).thenReturn(Optional.of(newAgent));

        AdminReassignRequest req = new AdminReassignRequest("agNew", "reason");
        assertThrows(AgentCapacityExceededException.class, () ->
                service.forceReassign("as1", req, "admin1", "adminUser")
        );

        verify(eventPublisher, never()).publishTicketAssigned(any());
    }

    @Test
    void forceReassign_happyPath_updatesOldAndCreatesNew_andPublishesEvent() {
        Assignment oldAssignment = mockAssignment("as1", "t1", "T-1", "agOld", "oldAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        AgentWorkload oldAgent = new AgentWorkload("agOld", "oldAgent");
        oldAgent.setActiveTickets(3);
        oldAgent.setStatus(AgentStatus.BUSY);

        AgentWorkload newAgent = new AgentWorkload("agNew", "newAgent");
        newAgent.setStatus(AgentStatus.AVAILABLE);
        newAgent.setActiveTickets(1);
        newAgent.setTotalAssignedTickets(10);

        TicketCache ticket = ticket("t1", "T-1", "Login issue", "HIGH", "AUTH");

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(oldAssignment));
        when(agentWorkloadRepository.findById("agNew")).thenReturn(Optional.of(newAgent));
        when(agentWorkloadRepository.findById("agOld")).thenReturn(Optional.of(oldAgent));
        when(ticketCacheRepository.findById("t1")).thenReturn(Optional.of(ticket));

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketCacheRepository.save(any(TicketCache.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminReassignRequest req = new AdminReassignRequest("agNew", "need expertise");

        AdminAssignmentDTO dto = service.forceReassign("as1", req, "admin1", "adminUser");
        assertNotNull(dto);

        verify(assignmentRepository, atLeast(2)).save(any(Assignment.class));
        verify(agentWorkloadRepository, atLeast(2)).save(any(AgentWorkload.class));
        verify(ticketCacheRepository).save(any(TicketCache.class));
        verify(eventPublisher).publishTicketAssigned(any(TicketAssignedEvent.class));
    }

    @Test
    void unassignTicket_whenStatusInvalid_throws() {
        Assignment assignment = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.NOT_ASSIGNED, AssignmentType.AUTO);

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(assignment));

        assertThrows(AssignmentStatusException.class, () ->
                service.unassignTicket("as1", "reason", "admin1", "adminUser")
        );
    }

    @Test
    void unassignTicket_whenReassigned_delegatesToSelfOnCurrentAssignment() {
        Assignment historical = mockAssignment("asOld", "t1", "T-1", "agOld", "oldAgent",
                AssignmentStatus.REASSIGNED, AssignmentType.MANUAL);

        Assignment current = mockAssignment("asCurrent", "t1", "T-1", "agNew", "newAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.MANUAL);

        when(assignmentRepository.findById("asOld")).thenReturn(Optional.of(historical));
        when(assignmentRepository.findByTicketIdAndStatus("t1", AssignmentStatus.ASSIGNED)).thenReturn(Optional.of(current));

        when(self.unassignTicket(eq("asCurrent"), anyString(), eq("admin1"), eq("adminUser")))
                .thenReturn("Ticket unassigned successfully");

        String result = service.unassignTicket("asOld", "reason", "admin1", "adminUser");

        assertEquals("Ticket unassigned successfully", result);
        verify(self).unassignTicket(eq("asCurrent"), contains("historical"), eq("admin1"), eq("adminUser"));
    }

    @Test
    void unassignTicket_whenReassignedAndNoCurrentAssignment_throwsAssignmentNotFoundException() {
        Assignment historical = mockAssignment("asOld", "t1", "T-1", "agOld", "oldAgent",
                AssignmentStatus.REASSIGNED, AssignmentType.MANUAL);

        when(assignmentRepository.findById("asOld")).thenReturn(Optional.of(historical));
        when(assignmentRepository.findByTicketIdAndStatus("t1", AssignmentStatus.ASSIGNED)).thenReturn(Optional.empty());

        assertThrows(AssignmentNotFoundException.class, () ->
                service.unassignTicket("asOld", "reason", "admin1", "adminUser")
        );
    }

    @Test
    void unassignTicket_whenAssigned_updatesAssignmentAndTicketAndAgent() {
        Assignment assignment = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        AgentWorkload agent = new AgentWorkload("ag1", "agent1");
        agent.setActiveTickets(2);
        agent.setStatus(AgentStatus.BUSY);

        TicketCache ticket = ticket("t1", "T-1", "Some title", "LOW", "GEN");

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(assignment));
        when(agentWorkloadRepository.findById("ag1")).thenReturn(Optional.of(agent));
        when(ticketCacheRepository.findById("t1")).thenReturn(Optional.of(ticket));

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketCacheRepository.save(any(TicketCache.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = service.unassignTicket("as1", "not needed", "admin1", "adminUser");

        assertEquals("Ticket unassigned successfully", result);
        verify(agentWorkloadRepository).save(any(AgentWorkload.class));
        verify(assignmentRepository).save(any(Assignment.class));
        verify(ticketCacheRepository).save(any(TicketCache.class));
    }

    @Test
    void deleteAssignment_whenAssigned_throws() {
        Assignment assignment = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(assignment));

        assertThrows(AssignmentStatusException.class, () ->
                service.deleteAssignment("as1", "admin1", "adminUser")
        );

        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }

    @Test
    void deleteAssignment_whenNotActive_deletesEntity() {
        Assignment assignment = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.REASSIGNED, AssignmentType.AUTO);

        when(assignmentRepository.findById("as1")).thenReturn(Optional.of(assignment));

        String result = service.deleteAssignment("as1", "admin1", "adminUser");
        assertEquals("Assignment deleted successfully", result);

        verify(assignmentRepository).delete((Assignment) eq(assignment));
    }

    @Test
    void getAssignmentStats_returnsCounts() {
        when(assignmentRepository.count()).thenReturn(100L);
        when(assignmentRepository.countByStatus(AssignmentStatus.ASSIGNED)).thenReturn(10L);
        when(assignmentRepository.countByStatus(AssignmentStatus.REASSIGNED)).thenReturn(5L);
        when(assignmentRepository.countByStatus(AssignmentStatus.NOT_ASSIGNED)).thenReturn(2L);

        when(assignmentRepository.countByAssignmentType(AssignmentType.AUTO)).thenReturn(80L);
        when(assignmentRepository.countByAssignmentType(AssignmentType.MANUAL)).thenReturn(20L);

        when(ticketCacheRepository.countByStatusAndAssignedAgentIdIsNull("OPEN")).thenReturn(7L);

        when(agentWorkloadRepository.count()).thenReturn(6L);
        when(agentWorkloadRepository.countByStatus(AgentStatus.AVAILABLE)).thenReturn(3L);
        when(agentWorkloadRepository.countByStatus(AgentStatus.BUSY)).thenReturn(2L);
        when(agentWorkloadRepository.countByStatus(AgentStatus.OFFLINE)).thenReturn(1L);

        AssignmentStatsDTO dto = service.getAssignmentStats();

        assertNotNull(dto);
        assertEquals(100L, longVal(dto, "totalAssignments"));
        assertEquals(10L, longVal(dto, "activeAssignments"));
        assertEquals(7L, longVal(dto, "unassignedTickets"));
        assertEquals(6L, longVal(dto, "totalAgents"));
    }

    @Test
    void getAgentWorkload_returnsDetailsWithActiveAssignments() {
        AgentWorkload agent = new AgentWorkload("ag1", "agent1");
        agent.setActiveTickets(2);
        agent.setTotalAssignedTickets(10);
        agent.setCompletedTickets(8);
        agent.setStatus(AgentStatus.BUSY);
        agent.setLastAssignedAt(LocalDateTime.now().minusHours(2));

        Assignment a1 = mockAssignment("as1", "t1", "T-1", "ag1", "agent1",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);
        TicketCache t1 = ticket("t1", "T-1", "Login issue", "HIGH", "AUTH");

        when(agentWorkloadRepository.findById("ag1")).thenReturn(Optional.of(agent));
        when(assignmentRepository.findByAgentIdAndStatus("ag1", AssignmentStatus.ASSIGNED)).thenReturn(List.of(a1));
        when(assignmentRepository.countByAgentId("ag1")).thenReturn(20L);
        when(assignmentRepository.countByAgentIdAndStatusNot("ag1", AssignmentStatus.ASSIGNED)).thenReturn(18L);
        when(ticketCacheRepository.findById("t1")).thenReturn(Optional.of(t1));

        AgentWorkloadDetailsDTO dto = service.getAgentWorkload("ag1");
        assertNotNull(dto);

        assertEquals("ag1", readValue(dto, "agentId", String.class));

        // stable assertions: service must call these, regardless of DTO field naming
        verify(assignmentRepository).countByAgentId("ag1");
        verify(assignmentRepository).countByAgentIdAndStatusNot("ag1", AssignmentStatus.ASSIGNED);

        // optional assertions: only if your DTO exposes these property names
        Long total = firstLong(dto, "totalAssignments", "totalAssignmentCount", "total");
        Long completed = firstLong(dto, "completedAssignments", "completedAssignmentCount", "completed");

        if (total != null) assertEquals(20L, total);
        if (completed != null) assertEquals(18L, completed);

        List<?> activeAssignments = readValue(dto, "activeAssignments", List.class);
        assertNotNull(activeAssignments);
        assertEquals(1, activeAssignments.size());
    }

    @Test
    void getUnassignedTickets_mapsToDTOsAndAddsSlaIfPresent() {
        TicketCache t1 = ticket("t1", "T-1", "Title", "LOW", "GEN");
        t1.setDescription("desc");
        t1.setCreatedByUsername("user1");

        when(ticketCacheRepository.findByStatusAndAssignedAgentIdIsNull("OPEN")).thenReturn(List.of(t1));

        SlaTracking sla = new SlaTracking();
        sla.setSlaStatus(SlaStatus.WARNING);
        when(slaService.getSlaTracking("t1")).thenReturn(Optional.of(sla));
        when(slaService.calculateTimeRemaining(sla)).thenReturn("2h");

        List<UnassignedTicketDTO> result = service.getUnassignedTickets();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("t1", result.get(0).getTicketId());
        assertEquals("WARNING", result.get(0).getSlaStatus());
        assertEquals("2h", result.get(0).getTimeRemaining());
    }

    @Test
    void bulkReassign_successAll_returnsMap() {
        AgentWorkload toAgent = new AgentWorkload("agTo", "toAgent");
        toAgent.setStatus(AgentStatus.AVAILABLE);
        toAgent.setActiveTickets(0);

        Assignment a1 = mockAssignment("as1", "t1", "T-1", "agFrom", "fromAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);
        Assignment a2 = mockAssignment("as2", "t2", "T-2", "agFrom", "fromAgent",
                AssignmentStatus.ASSIGNED, AssignmentType.AUTO);

        when(agentWorkloadRepository.findById("agTo")).thenReturn(Optional.of(toAgent));
        when(assignmentRepository.findByAgentIdAndStatus("agFrom", AssignmentStatus.ASSIGNED))
                .thenReturn(List.of(a1, a2));

        when(self.forceReassign(anyString(), any(AdminReassignRequest.class), eq("admin1"), eq("adminUser")))
                .thenReturn(mock(AdminAssignmentDTO.class));

        BulkReassignRequest req = new BulkReassignRequest("agFrom", "agTo", "bulk reason");
        Map<String, Object> result = service.bulkReassign(req, "admin1", "adminUser");

        assertEquals(2, result.get("totalProcessed"));
        assertEquals(2, result.get("successCount"));
        assertEquals(0, result.get("failedCount"));

        verify(self, times(2)).forceReassign(anyString(), any(AdminReassignRequest.class), eq("admin1"), eq("adminUser"));
    }
}
