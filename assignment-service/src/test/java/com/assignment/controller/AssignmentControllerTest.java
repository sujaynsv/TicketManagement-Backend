package com.assignment.controller;

import com.assignment.dto.*;
import com.assignment.entity.AssignmentStatus;
import com.assignment.entity.AssignmentType;
import com.assignment.repository.AssignmentRepository;
import com.assignment.service.AssignmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AssignmentController.class,
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
class AssignmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AssignmentService assignmentService;
    @MockBean private AssignmentRepository assignmentRepository;

    // Key fix: disables @Valid-triggered validation in this MVC slice test
    @MockBean private LocalValidatorFactoryBean validator;

    @Test
    void getUnassignedTickets_returnsOkList() throws Exception {
        UnassignedTicketDTO dto = new UnassignedTicketDTO();
        dto.setTicketId("t1");
        dto.setTicketNumber("T-1");
        dto.setTitle("Issue");

        when(assignmentService.getUnassignedTickets()).thenReturn(List.of(dto));

        mockMvc.perform(get("/assignments/tickets/unassigned")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value("t1"));

        verify(assignmentService).getUnassignedTickets();
    }

    @Test
    void getAvailableAgents_returnsOkList() throws Exception {
        AgentWorkloadDTO dto = new AgentWorkloadDTO();
        dto.setAgentId("a1");
        dto.setAgentUsername("agent1");

        when(assignmentService.getAvailableAgents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/assignments/agents/available")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].agentId").value("a1"));

        verify(assignmentService).getAvailableAgents();
    }

    @Test
    void manualAssignment_returns201_andCallsService() throws Exception {
        // Build JSON body (field names can be adjusted to match ManualAssignmentRequest)
        ObjectNode body = objectMapper.createObjectNode();
        body.put("ticketId", "t1");
        body.put("agentId", "a1");
        body.put("reason", "manual assign");

        AssignmentDTO response = new AssignmentDTO();
        response.setAssignmentId("as1");
        response.setTicketId("t1");
        response.setAgentId("a1");

        when(assignmentService.manualAssignment(any(ManualAssignmentRequest.class), eq("mgr1"), eq("manager")))
                .thenReturn(response);

        mockMvc.perform(post("/assignments/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-User-Id", "mgr1")
                        .header("X-Username", "manager")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value("as1"))
                .andExpect(jsonPath("$.ticketId").value("t1"))
                .andExpect(jsonPath("$.agentId").value("a1"));

        verify(assignmentService).manualAssignment(any(ManualAssignmentRequest.class), eq("mgr1"), eq("manager"));
    }

    @Test
    void getAgentTickets_returnsOkList() throws Exception {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setAssignmentId("as1");
        dto.setAgentId("a1");
        dto.setTicketId("t1");

        when(assignmentService.getAgentTickets("a1")).thenReturn(List.of(dto));

        mockMvc.perform(get("/assignments/agents/{agentId}/tickets", "a1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value("t1"));

        verify(assignmentService).getAgentTickets("a1");
    }

    @Test
    void reassignTickets_success_returnsOk() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("ticketId", "t1");
        body.put("newAgentId", "a2");

        doNothing().when(assignmentService).reassignTicket("t1", "a2", "mgr1", "manager");

        mockMvc.perform(put("/assignments/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-User-Id", "mgr1")
                        .header("X-Username", "manager")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("Ticket Reassigned successfully"));

        verify(assignmentService).reassignTicket("t1", "a2", "mgr1", "manager");
    }

    @Test
    void reassignTickets_failure_returns400() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("ticketId", "t1");
        body.put("newAgentId", "a2");

        doThrow(new RuntimeException("bad request")).when(assignmentService)
                .reassignTicket("t1", "a2", "mgr1", "manager");

        mockMvc.perform(put("/assignments/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-User-Id", "mgr1")
                        .header("X-Username", "manager")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Reassignment Failed:")));

        verify(assignmentService).reassignTicket("t1", "a2", "mgr1", "manager");
    }

    @Test
    void getAssignmentByTicketId_found_returnsOkDto() throws Exception {
        var a = mock(com.assignment.entity.Assignment.class);
        when(a.getAssignmentId()).thenReturn("as1");
        when(a.getTicketId()).thenReturn("t1");
        when(a.getTicketNumber()).thenReturn("T-1");
        when(a.getAgentId()).thenReturn("a1");
        when(a.getAgentUsername()).thenReturn("agent1");
        when(a.getAssignedBy()).thenReturn("mgr1");
        when(a.getAssignedByUsername()).thenReturn("manager");
        when(a.getAssignmentType()).thenReturn(AssignmentType.MANUAL);
        when(a.getAssignedAt()).thenReturn(LocalDateTime.now().minusMinutes(30));
        when(a.getStatus()).thenReturn(AssignmentStatus.ASSIGNED);

        when(assignmentRepository.findByTicketIdAndStatus("t1", AssignmentStatus.ASSIGNED))
                .thenReturn(Optional.of(a));

        mockMvc.perform(get("/assignments/ticket/{ticketId}", "t1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentId").value("as1"))
                .andExpect(jsonPath("$.ticketId").value("t1"))
                .andExpect(jsonPath("$.agentId").value("a1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(assignmentRepository).findByTicketIdAndStatus("t1", AssignmentStatus.ASSIGNED);
    }

    @Test
    void getAssignmentByTicketId_notFound_throwsServletException() {
        when(assignmentRepository.findByTicketIdAndStatus("missing", AssignmentStatus.ASSIGNED))
                .thenReturn(Optional.empty());

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/assignments/ticket/{ticketId}", "missing")
                        .accept(MediaType.APPLICATION_JSON))
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("No active assignment found for ticket"));

        verify(assignmentRepository).findByTicketIdAndStatus("missing", AssignmentStatus.ASSIGNED);
    }

    @Test
    void getMyAssignments_returnsOkList() throws Exception {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setAssignmentId("as1");
        dto.setTicketId("t1");

        when(assignmentService.getAgentAssignments("a1")).thenReturn(List.of(dto));

        mockMvc.perform(get("/assignments/my")
                        .header("X-User-Id", "a1")
                        .header("X-Username", "agent1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value("t1"));

        verify(assignmentService).getAgentAssignments("a1");
    }
}
