package com.assignment.service;

import com.assignment.client.AuthServiceClient;
import com.assignment.dto.AgentWorkloadDTO;
import com.assignment.dto.AssignmentDTO;
import com.assignment.dto.ManualAssignmentRequest;
import com.assignment.dto.UnassignedTicketDTO;
import com.assignment.entity.*;
import com.assignment.exception.AgentCapacityExceededException;
import com.assignment.exception.AgentOfflineException;
import com.assignment.exception.TicketAlreadyAssignedException;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.ticket.event.TicketAssignedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;


import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {

    @Mock private TicketCacheRepository ticketCacheRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AgentWorkloadRepository agentWorkloadRepository;
    @Mock private SlaService slaService;
    @Mock private EventPublisher eventPublisher;

    // self proxy (used by autoAssignTicket -> self.syncAgentsFromAuthService())
    @Mock private AssignmentService self;

    private AssignmentService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AssignmentService(
                ticketCacheRepository,
                authServiceClient,
                assignmentRepository,
                agentWorkloadRepository,
                slaService,
                eventPublisher,
                self
        );

        // set @Value fields
        setField(service, "autoAssignEnabled", true);
        setField(service, "assignmentStrategy", "LEAST_LOADED");
        setField(service, "maxTicketsPerAgent", 5);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ---------------- getUnassignedTickets ----------------

    @Test
    void getUnassignedTickets_mapsTickets_andAddsSlaIfPresent() {
        TicketCache t = new TicketCache();
        t.setTicketId("TKT-1");
        t.setTicketNumber("T-1");
        t.setTitle("Title");
        t.setDescription("Desc");
        t.setCategory("GEN");
        t.setPriority("LOW");
        t.setStatus("OPEN");
        t.setCreatedByUsername("user1");
        t.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(ticketCacheRepository.findByStatusInAndAssignedAgentIdIsNull(List.of("OPEN")))
                .thenReturn(List.of(t));

        SlaTracking sla = new SlaTracking();
        sla.setSlaStatus(SlaStatus.WARNING);

        when(slaService.getSlaTracking("TKT-1")).thenReturn(Optional.of(sla));
        when(slaService.calculateTimeRemaining(sla)).thenReturn("2h");

        List<UnassignedTicketDTO> res = service.getUnassignedTickets();

        assertEquals(1, res.size());
        assertEquals("TKT-1", res.get(0).getTicketId());
        assertEquals("WARNING", res.get(0).getSlaStatus());
        assertEquals("2h", res.get(0).getTimeRemaining());
    }

    @Test
    void getUnassignedTickets_mapsTickets_noSla() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("T-1");
        ticket.setTicketNumber("T-1");
        ticket.setStatus("OPEN");          // ⭐ REQUIRED
        ticket.setAssignedAgentId(null);   // ⭐ REQUIRED
        ticket.setCategory("CAT");
        ticket.setPriority(null);          // no SLA case
        ticket.setCreatedAt(LocalDateTime.now());

        when(ticketCacheRepository
                .findByStatusInAndAssignedAgentIdIsNull(List.of("OPEN")))
            .thenReturn(List.of(ticket));


        when(slaService.getSlaTracking("TKT-1")).thenReturn(Optional.empty());

        List<UnassignedTicketDTO> res = service.getUnassignedTickets();

        assertEquals(1, res.size());
        assertNull(res.get(0).getSlaStatus());
        assertNull(res.get(0).getTimeRemaining());
    }

    // ---------------- getAvailableAgents ----------------

    @Test
    void getAvailableAgents_filtersAndSorts_andMarksRecommended() {
        // workloads in DB
        AgentWorkload w1 = new AgentWorkload("a1", "agent1");
        w1.setActiveTickets(3);
        w1.setTotalAssignedTickets(10);
        w1.setCompletedTickets(7);
        w1.setStatus(AgentStatus.AVAILABLE);

        AgentWorkload w2 = new AgentWorkload("a2", "agent2");
        w2.setActiveTickets(1);
        w2.setTotalAssignedTickets(4);
        w2.setCompletedTickets(3);
        w2.setStatus(AgentStatus.AVAILABLE);

        when(agentWorkloadRepository.findAll()).thenReturn(List.of(w1, w2));

        // auth service agents
        AuthServiceClient.AgentDTO a1 = mock(AuthServiceClient.AgentDTO.class);
        when(a1.getUserId()).thenReturn("a1");
        when(a1.getUsername()).thenReturn("agent1");
        when(a1.getRole()).thenReturn("SUPPORT_AGENT");
        when(a1.getIsActive()).thenReturn(true);

        AuthServiceClient.AgentDTO a2 = mock(AuthServiceClient.AgentDTO.class);
        when(a2.getUserId()).thenReturn("a2");
        when(a2.getUsername()).thenReturn("agent2");
        when(a2.getRole()).thenReturn("SUPPORT_AGENT");
        when(a2.getIsActive()).thenReturn(true);

        // should be skipped (not support agent)
        AuthServiceClient.AgentDTO mgr = mock(AuthServiceClient.AgentDTO.class);
        when(mgr.getUserId()).thenReturn("m1");
        when(mgr.getUsername()).thenReturn("manager");
        when(mgr.getRole()).thenReturn("MANAGER");
        when(mgr.getIsActive()).thenReturn(true);

        when(authServiceClient.getAllAgents()).thenReturn(List.of(a1, a2, mgr));

        List<AgentWorkloadDTO> res = service.getAvailableAgents();

        // only 2 support agents
        assertEquals(2, res.size());

        // sorted by activeTickets ascending: agent2 (1) then agent1 (3)
        assertEquals("a2", res.get(0).getAgentId());
        assertEquals(1, res.get(0).getActiveTickets());
        assertEquals("a1", res.get(1).getAgentId());

        // recommended should be first AVAILABLE after sort
        assertTrue(Boolean.TRUE.equals(res.get(0).getIsRecommended()));
        assertFalse(Boolean.TRUE.equals(res.get(1).getIsRecommended()));
    }

    @Test
    void getAvailableAgents_agentWithoutWorkload_defaultsToAvailableAndZero() {
        when(agentWorkloadRepository.findAll()).thenReturn(List.of());

        AuthServiceClient.AgentDTO a3 = mock(AuthServiceClient.AgentDTO.class);
        when(a3.getUserId()).thenReturn("a3");
        when(a3.getUsername()).thenReturn("agent3");
        when(a3.getRole()).thenReturn("SUPPORT_AGENT");
        when(a3.getIsActive()).thenReturn(true);

        when(authServiceClient.getAllAgents()).thenReturn(List.of(a3));

        List<AgentWorkloadDTO> res = service.getAvailableAgents();

        assertEquals(1, res.size());
        assertEquals("a3", res.get(0).getAgentId());
        assertEquals(0, res.get(0).getActiveTickets());
        assertEquals("AVAILABLE", res.get(0).getStatus());
        assertTrue(Boolean.TRUE.equals(res.get(0).getIsRecommended()));
    }

    // ---------------- manualAssignment ----------------

    @Test
    void manualAssignment_ticketAlreadyAssigned_throws() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setAssignedAgentId("aOld");
        ticket.setAssignedAgentUsername("oldUser");

        ManualAssignmentRequest req = new ManualAssignmentRequest();
        req.setTicketId("TKT-1");
        req.setAgentId("a1");
        req.setPriority("HIGH");

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));

        assertThrows(TicketAlreadyAssignedException.class,
                () -> service.manualAssignment(req, "mgr1", "manager"));

        verifyNoInteractions(agentWorkloadRepository);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void manualAssignment_agentOffline_throws() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setPriority("LOW");

        ManualAssignmentRequest req = new ManualAssignmentRequest();
        req.setTicketId("TKT-1");
        req.setAgentId("a1");
        req.setPriority("HIGH");

        AgentWorkload agent = new AgentWorkload("a1", "agent1");
        agent.setStatus(AgentStatus.OFFLINE);
        agent.setActiveTickets(0);

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));

        assertThrows(AgentOfflineException.class,
                () -> service.manualAssignment(req, "mgr1", "manager"));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void manualAssignment_agentAtCapacity_throws() throws Exception {
        setField(service, "maxTicketsPerAgent", 2);

        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");

        ManualAssignmentRequest req = new ManualAssignmentRequest();
        req.setTicketId("TKT-1");
        req.setAgentId("a1");
        req.setPriority("HIGH");

        AgentWorkload agent = new AgentWorkload("a1", "agent1");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setActiveTickets(2);

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));

        assertThrows(AgentCapacityExceededException.class,
                () -> service.manualAssignment(req, "mgr1", "manager"));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void manualAssignment_success_updatesTicketAndAgent_createsSla_andPublishesEvent() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setTitle("Title");
        ticket.setDescription("Desc");
        ticket.setCategory("GEN");
        ticket.setPriority("LOW");
        ticket.setStatus("OPEN");
        ticket.setCreatedByUsername("creator");

        AgentWorkload agent = new AgentWorkload("a1", "agent1");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setActiveTickets(1);
        agent.setTotalAssignedTickets(5);
        agent.setCompletedTickets(2);

        ManualAssignmentRequest req = new ManualAssignmentRequest();
        req.setTicketId("TKT-1");
        req.setAgentId("a1");
        req.setPriority("HIGH");
        req.setAssignmentNote("note");

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));

        // return a mocked saved assignment so convertToAssignmentDTO is stable
        Assignment saved = mock(Assignment.class);
        when(saved.getAssignmentId()).thenReturn("AS-1");
        when(saved.getTicketId()).thenReturn("TKT-1");
        when(saved.getTicketNumber()).thenReturn("T-1");
        when(saved.getAgentId()).thenReturn("a1");
        when(saved.getAgentUsername()).thenReturn("agent1");
        when(saved.getAssignedBy()).thenReturn("mgr1");
        when(saved.getAssignedByUsername()).thenReturn("manager");
        when(saved.getAssignmentType()).thenReturn(AssignmentType.MANUAL);
        when(saved.getStatus()).thenReturn(AssignmentStatus.ASSIGNED);
        when(saved.getAssignedAt()).thenReturn(LocalDateTime.now());
        when(saved.getCompletedAt()).thenReturn(null);
        when(saved.getTicketTitle()).thenReturn("Title");
        when(saved.getTicketDescription()).thenReturn("Desc");
        when(saved.getTicketStatus()).thenReturn("ASSIGNED");
        when(saved.getTicketPriority()).thenReturn("HIGH");
        when(saved.getTicketCategory()).thenReturn("GEN");
        when(saved.getCreatedByUsername()).thenReturn("creator");

        when(assignmentRepository.save(any(Assignment.class))).thenReturn(saved);
        when(ticketCacheRepository.save(any(TicketCache.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentDTO dto = service.manualAssignment(req, "mgr1", "manager");
        assertNotNull(dto);
        assertEquals("AS-1", dto.getAssignmentId());
        assertEquals("TKT-1", dto.getTicketId());
        assertEquals("a1", dto.getAgentId());

        // ticket updated
        assertEquals("a1", ticket.getAssignedAgentId());
        assertEquals("agent1", ticket.getAssignedAgentUsername());
        assertEquals("ASSIGNED", ticket.getStatus());
        assertEquals("HIGH", ticket.getPriority());

        // agent updated
        assertEquals(2, agent.getActiveTickets());
        assertEquals(6, agent.getTotalAssignedTickets());

        verify(slaService).createSlaTracking("TKT-1", "T-1", "HIGH", "GEN");
        verify(eventPublisher).publishTicketAssigned(any(TicketAssignedEvent.class));
    }

    // ---------------- autoAssignTicket ----------------

    @Test
    void autoAssignTicket_disabled_doesNothing() throws Exception {
        setField(service, "autoAssignEnabled", false);

        service.autoAssignTicket("TKT-1");

        verifyNoInteractions(ticketCacheRepository);
        verifyNoInteractions(self);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void autoAssignTicket_ticketNotInCache_doesNothing() {
        when(ticketCacheRepository.findById("TKT-404")).thenReturn(Optional.empty());

        service.autoAssignTicket("TKT-404");

        verify(ticketCacheRepository).findById("TKT-404");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void autoAssignTicket_alreadyAssigned_doesNothing() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setAssignedAgentId("a1");

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));

        service.autoAssignTicket("TKT-1");

        verify(ticketCacheRepository).findById("TKT-1");
        verifyNoInteractions(self);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void autoAssignTicket_noAvailableAgents_doesNothing() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setStatus("OPEN");

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));
        doNothing().when(self).syncAgentsFromAuthService();
        when(agentWorkloadRepository.findByStatusOrderByActiveTicketsAsc(AgentStatus.AVAILABLE))
                .thenReturn(List.of());

        service.autoAssignTicket("TKT-1");

        verify(self).syncAgentsFromAuthService();
        verifyNoInteractions(eventPublisher);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void autoAssignTicket_success_assignsAndPublishesEvent() {
        TicketCache ticket = new TicketCache();
        ticket.setTicketId("TKT-1");
        ticket.setTicketNumber("T-1");
        ticket.setTitle("Title");
        ticket.setDescription("Desc");
        ticket.setCategory("GEN");
        ticket.setPriority("LOW");
        ticket.setStatus("OPEN");
        ticket.setCreatedByUsername("creator");

        when(ticketCacheRepository.findById("TKT-1")).thenReturn(Optional.of(ticket));

        doNothing().when(self).syncAgentsFromAuthService();

        AgentWorkload agent = new AgentWorkload("a1", "agent1");
        agent.setStatus(AgentStatus.AVAILABLE);
        agent.setActiveTickets(0);
        agent.setTotalAssignedTickets(1);

        when(agentWorkloadRepository.findByStatusOrderByActiveTicketsAsc(AgentStatus.AVAILABLE))
                .thenReturn(List.of(agent));

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketCacheRepository.save(any(TicketCache.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));

        service.autoAssignTicket("TKT-1");

        verify(self).syncAgentsFromAuthService();
        verify(assignmentRepository).save(any(Assignment.class));

        assertEquals("a1", ticket.getAssignedAgentId());
        assertEquals("agent1", ticket.getAssignedAgentUsername());
        assertEquals("ASSIGNED", ticket.getStatus());

        assertEquals(1, agent.getActiveTickets());
        assertEquals(2, agent.getTotalAssignedTickets());

        verify(eventPublisher).publishTicketAssigned(any(TicketAssignedEvent.class));
    }

    // ---------------- completeAssignment ----------------

    @Test
    void completeAssignment_noActiveAssignment_returns() {
        when(assignmentRepository.findByTicketIdAndStatus("TKT-1", AssignmentStatus.ASSIGNED))
                .thenReturn(Optional.empty());

        service.completeAssignment("TKT-1");

        verify(assignmentRepository).findByTicketIdAndStatus("TKT-1", AssignmentStatus.ASSIGNED);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void completeAssignment_success_updatesAssignmentAndAgent() {
        Assignment assignment = mock(Assignment.class);
        when(assignment.getTicketId()).thenReturn("TKT-1");
        when(assignment.getTicketNumber()).thenReturn("T-1");
        when(assignment.getAgentId()).thenReturn("a1");

        when(assignmentRepository.findByTicketIdAndStatus("TKT-1", AssignmentStatus.ASSIGNED))
                .thenReturn(Optional.of(assignment));

        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AgentWorkload agent = new AgentWorkload("a1", "agent1");
        agent.setActiveTickets(2);
        agent.setCompletedTickets(3);
        agent.setStatus(AgentStatus.BUSY);

        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));
        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));

        service.completeAssignment("TKT-1");

        verify(assignmentRepository).save(assignment);
        verify(assignment).setTicketStatus("RESOLVED");
        verify(assignment).setCompletedAt(any(LocalDateTime.class));

        assertEquals(1, agent.getActiveTickets());
        assertEquals(4, agent.getCompletedTickets());
        assertEquals(AgentStatus.AVAILABLE, agent.getStatus());
    }

    // ---------------- syncAgentsFromAuthService ----------------

    @Test
    void syncAgentsFromAuthService_createsMissingAgent_andUpdatesUsername() {
        AuthServiceClient.AgentDTO a1 = mock(AuthServiceClient.AgentDTO.class);
        when(a1.getUserId()).thenReturn("a1");
        when(a1.getUsername()).thenReturn("agentOne");
        when(a1.getRole()).thenReturn("SUPPORT_AGENT");
        when(a1.getIsActive()).thenReturn(true);

        AuthServiceClient.AgentDTO a2 = mock(AuthServiceClient.AgentDTO.class);
        when(a2.getUserId()).thenReturn("a2");
        when(a2.getUsername()).thenReturn("agentTwoNEW");
        when(a2.getRole()).thenReturn("SUPPORT_AGENT");
        when(a2.getIsActive()).thenReturn(true);

        when(authServiceClient.getAllAgents()).thenReturn(List.of(a1, a2));

        // a1 missing -> create
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.empty());

        // a2 exists with old username -> update
        AgentWorkload existing = new AgentWorkload("a2", "agentTwoOLD");
        existing.setStatus(AgentStatus.AVAILABLE);
        when(agentWorkloadRepository.findById("a2")).thenReturn(Optional.of(existing));

        when(agentWorkloadRepository.save(any(AgentWorkload.class))).thenAnswer(inv -> inv.getArgument(0));

        service.syncAgentsFromAuthService();

        ArgumentCaptor<AgentWorkload> captor = ArgumentCaptor.forClass(AgentWorkload.class);
        verify(agentWorkloadRepository, atLeastOnce()).save(captor.capture());

        List<AgentWorkload> saved = captor.getAllValues();

        // ensure a1 was created
        assertTrue(saved.stream().anyMatch(x -> "a1".equals(x.getAgentId())));
        // ensure a2 username updated
        assertEquals("agentTwoNEW", existing.getAgentUsername());
    }
}