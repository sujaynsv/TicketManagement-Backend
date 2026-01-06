package com.ticket.controller;

import com.ticket.dto.*;
import com.ticket.service.AdminTicketService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminTicketController.class,
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
class AdminTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminTicketService adminTicketService;

    // Disables @Valid failures so {} bodies won’t 400 [web:389]
    @MockBean
    private LocalValidatorFactoryBean validator;

    @Test
    void getAllTickets_defaults_ok() throws Exception {
        when(adminTicketService.getAllTickets(any(TicketFilterRequest.class))).thenReturn(null);

        mockMvc.perform(get("/admin/tickets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getAllTickets(any(TicketFilterRequest.class));
    }

    @Test
    void getAllTickets_withParams_ok() throws Exception {
        when(adminTicketService.getAllTickets(any(TicketFilterRequest.class))).thenReturn(null);

        mockMvc.perform(get("/admin/tickets")
                        .param("page", "1")
                        .param("size", "5")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("category", "TECHNICAL_ISSUE")
                        .param("assignedToUserId", "agent1")
                        .param("createdByUserId", "u1")
                        .param("search", "login")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getAllTickets(any(TicketFilterRequest.class));
    }

    @Test
    void getTicketById_ok() throws Exception {
        when(adminTicketService.getTicketById("t1")).thenReturn(null);

        mockMvc.perform(get("/admin/tickets/{ticketId}", "t1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getTicketById("t1");
    }

    @Test
    void changePriority_ok() throws Exception {
        when(adminTicketService.changePriority(eq("t1"), any(AdminChangePriorityRequest.class), eq("admin1"), eq("adminUser")))
                .thenReturn(null);

        mockMvc.perform(put("/admin/tickets/{ticketId}/priority", "t1")
                        .header("X-User-Id", "admin1")
                        .header("X-Username", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).changePriority(eq("t1"), any(AdminChangePriorityRequest.class), eq("admin1"), eq("adminUser"));
    }

    @Test
    void changeCategory_ok() throws Exception {
        when(adminTicketService.changeCategory(eq("t1"), any(AdminChangeCategoryRequest.class), eq("admin1"), eq("adminUser")))
                .thenReturn(null);

        mockMvc.perform(put("/admin/tickets/{ticketId}/category", "t1")
                        .header("X-User-Id", "admin1")
                        .header("X-Username", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).changeCategory(eq("t1"), any(AdminChangeCategoryRequest.class), eq("admin1"), eq("adminUser"));
    }

    @Test
    void changeStatus_ok() throws Exception {
        when(adminTicketService.changeStatus(eq("t1"), any(AdminChangeStatusRequest.class), eq("admin1"), eq("adminUser")))
                .thenReturn(null);

        mockMvc.perform(put("/admin/tickets/{ticketId}/status", "t1")
                        .header("X-User-Id", "admin1")
                        .header("X-Username", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).changeStatus(eq("t1"), any(AdminChangeStatusRequest.class), eq("admin1"), eq("adminUser"));
    }

    @Test
    void deleteTicket_defaultSoftDelete_ok() throws Exception {
        when(adminTicketService.deleteTicket("t1", false, "admin1", "adminUser"))
                .thenReturn("Ticket closed (soft delete)");

        mockMvc.perform(delete("/admin/tickets/{ticketId}", "t1")
                        .header("X-User-Id", "admin1")
                        .header("X-Username", "adminUser")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Ticket closed")));

        verify(adminTicketService).deleteTicket("t1", false, "admin1", "adminUser");
    }

    @Test
    void deleteTicket_hardDelete_true_ok() throws Exception {
        when(adminTicketService.deleteTicket("t1", true, "admin1", "adminUser"))
                .thenReturn("Ticket permanently deleted");

        mockMvc.perform(delete("/admin/tickets/{ticketId}", "t1")
                        .param("hardDelete", "true")
                        .header("X-User-Id", "admin1")
                        .header("X-Username", "adminUser")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("permanently")));

        verify(adminTicketService).deleteTicket("t1", true, "admin1", "adminUser");
    }

    @Test
    void getTicketStats_ok() throws Exception {
        when(adminTicketService.getTicketStats()).thenReturn(null);

        mockMvc.perform(get("/admin/tickets/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getTicketStats();
    }

    @Test
    void getUserTickets_ok() throws Exception {
        when(adminTicketService.getUserTickets("u1", 0, 10)).thenReturn(null);

        mockMvc.perform(get("/admin/tickets/user/{userId}", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getUserTickets("u1", 0, 10);
    }

    @Test
    void getAgentTickets_ok() throws Exception {
        when(adminTicketService.getAgentTickets("agent1", 0, 10)).thenReturn(null);

        mockMvc.perform(get("/admin/tickets/agent/{agentId}", "agent1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminTicketService).getAgentTickets("agent1", 0, 10);
    }
}
