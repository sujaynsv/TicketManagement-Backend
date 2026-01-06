package com.assignment.controller;

import com.assignment.entity.AgentWorkload;
import com.assignment.entity.Assignment;
import com.assignment.entity.TicketCache;
import com.assignment.repository.AgentWorkloadRepository;
import com.assignment.repository.AssignmentRepository;
import com.assignment.repository.TicketCacheRepository;
import com.assignment.service.AssignmentService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AgentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        },
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.config.discovery.enabled=false"
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentWorkloadRepository agentWorkloadRepository;

    @MockBean
    private AssignmentService assignmentService;

    @MockBean
    private AssignmentRepository assignmentRepository;

    @MockBean
    private TicketCacheRepository ticketCacheRepository;

    @Test
    void syncAgents_returnsOkMessage() throws Exception {
        doNothing().when(assignmentService).syncAgentsFromAuthService();

        mockMvc.perform(post("/agents/sync")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("Agents Successfully fetched from auth service."));

        verify(assignmentService).syncAgentsFromAuthService();
        verifyNoMoreInteractions(assignmentService);
    }

    @Test
    void updateAgentStatus_found_returnsOk() throws Exception {
        AgentWorkload agent = mock(AgentWorkload.class);
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));
        when(agentWorkloadRepository.save(agent)).thenReturn(agent);

        mockMvc.perform(put("/agents/{agentId}/status", "a1")
                        .param("status", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(agentWorkloadRepository).findById("a1");
        verify(agentWorkloadRepository).save(agent);
    }

    @Test
    void updateAgentStatus_notFound_throwsServletException() {
        when(agentWorkloadRepository.findById("missing")).thenReturn(Optional.empty());

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(put("/agents/{agentId}/status", "missing")
                        .param("status", "AVAILABLE")
                        .accept(MediaType.APPLICATION_JSON))
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Agent not found"));

        verify(agentWorkloadRepository).findById("missing");
        verify(agentWorkloadRepository, never()).save(any());
    }

    @Test
    void getAllAgents_returnsOkList() throws Exception {
        when(agentWorkloadRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/agents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(agentWorkloadRepository).findAll();
    }

    @Test
    void getAgent_found_returnsOk() throws Exception {
        AgentWorkload agent = mock(AgentWorkload.class);
        when(agentWorkloadRepository.findById("a1")).thenReturn(Optional.of(agent));

        mockMvc.perform(get("/agents/{agentId}", "a1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(agentWorkloadRepository).findById("a1");
    }

    @Test
    void getAgent_notFound_throwsServletException() {
        when(agentWorkloadRepository.findById("missing")).thenReturn(Optional.empty());

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/agents/{agentId}", "missing")
                        .accept(MediaType.APPLICATION_JSON))
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Agent not found"));

        verify(agentWorkloadRepository).findById("missing");
    }

    @Test
    void getAgentStats_countsLiveStatusesFromTicketCache() throws Exception {
        // Assignments for agent a1
        Assignment asg1 = mock(Assignment.class);
        when(asg1.getTicketNumber()).thenReturn("T-1");
        Assignment asg2 = mock(Assignment.class);
        when(asg2.getTicketNumber()).thenReturn("T-2");
        Assignment asg3 = mock(Assignment.class);
        when(asg3.getTicketNumber()).thenReturn("T-3");

        when(assignmentRepository.findByAgentId("a1")).thenReturn(List.of(asg1, asg2, asg3));

        // Ticket cache live statuses
        TicketCache tc1 = mock(TicketCache.class);
        when(tc1.getStatus()).thenReturn("ASSIGNED");
        when(tc1.getTicketNumber()).thenReturn("T-1");

        TicketCache tc2 = mock(TicketCache.class);
        when(tc2.getStatus()).thenReturn("IN_PROGRESS");
        when(tc2.getTicketNumber()).thenReturn("T-2");

        TicketCache tc3 = mock(TicketCache.class);
        when(tc3.getStatus()).thenReturn("RESOLVED");
        when(tc3.getTicketNumber()).thenReturn("T-3");

        when(ticketCacheRepository.findByTicketNumber("T-1")).thenReturn(Optional.of(tc1));
        when(ticketCacheRepository.findByTicketNumber("T-2")).thenReturn(Optional.of(tc2));
        when(ticketCacheRepository.findByTicketNumber("T-3")).thenReturn(Optional.of(tc3));

        mockMvc.perform(get("/agents/stats")
                        .header("X-User-Id", "a1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssigned").value(3))
                .andExpect(jsonPath("$.assigned").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.resolved").value(1))

                // controller doesn't return avgResolutionTime -> assert it's absent
                .andExpect(jsonPath("$.avgResolutionTime").doesNotExist());

        verify(assignmentRepository).findByAgentId("a1");
        verify(ticketCacheRepository).findByTicketNumber("T-1");
        verify(ticketCacheRepository).findByTicketNumber("T-2");
        verify(ticketCacheRepository).findByTicketNumber("T-3");
    }
}