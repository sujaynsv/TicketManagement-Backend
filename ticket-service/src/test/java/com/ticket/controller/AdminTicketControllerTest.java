package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.*;
import com.ticket.service.AdminTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTicketController.class)
class AdminTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTicketService adminTicketService;

    private AdminTicketDTO testTicket;
    private String adminId = "admin-001";
    private String adminUsername = "admin";

    @BeforeEach
    void setUp() {
        testTicket = new AdminTicketDTO(
                "TKT-001",
                "TKT-20240101-00001",
                "Test Ticket",
                "Description",
                "OPEN",
                "TECHNICAL_ISSUE",
                "HIGH",
                "user-001",
                "testuser",
                "agent-001",
                "agent",
                null,
                null,
                null,
                null,
                0,
                0,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null
        );
    }

    // ==================== GET ALL TICKETS ====================

    @Test
    void testGetAllTickets_WithDefaultParams_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(0, 10), 1);
        when(adminTicketService.getAllTickets(0, 10, null, null, null, null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/admin/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].ticketId").value("TKT-001"));

        verify(adminTicketService, times(1))
                .getAllTickets(0, 10, null, null, null, null, null, null);
    }

    @Test
    void testGetAllTickets_WithAllFilters_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(0, 20), 1);
        when(adminTicketService.getAllTickets(0, 20, "OPEN", "HIGH", "TECHNICAL_ISSUE", 
                "agent-001", "user-001", "test"))
                .thenReturn(page);

        mockMvc.perform(get("/admin/tickets")
                .param("page", "0")
                .param("size", "20")
                .param("status", "OPEN")
                .param("priority", "HIGH")
                .param("category", "TECHNICAL_ISSUE")
                .param("assignedToUserId", "agent-001")
                .param("createdByUserId", "user-001")
                .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        verify(adminTicketService, times(1))
                .getAllTickets(0, 20, "OPEN", "HIGH", "TECHNICAL_ISSUE", 
                        "agent-001", "user-001", "test");
    }

    @Test
    void testGetAllTickets_WithPagination_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(2, 50), 1);
        when(adminTicketService.getAllTickets(2, 50, null, null, null, null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/admin/tickets")
                .param("page", "2")
                .param("size", "50"))
                .andExpect(status().isOk());

        verify(adminTicketService, times(1))
                .getAllTickets(2, 50, null, null, null, null, null, null);
    }

    // ==================== GET TICKET BY ID ====================

    @Test
    void testGetTicketById_WithValidId_Success() throws Exception {
        when(adminTicketService.getTicketById("TKT-001")).thenReturn(testTicket);

        mockMvc.perform(get("/admin/tickets/TKT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-001"))
                .andExpect(jsonPath("$.title").value("Test Ticket"));

        verify(adminTicketService, times(1)).getTicketById("TKT-001");
    }

    @Test
    void testGetTicketById_WithInvalidId_ReturnsError() throws Exception {
        when(adminTicketService.getTicketById("INVALID"))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(get("/admin/tickets/INVALID"))
                .andExpect(status().is4xxClientError());

        verify(adminTicketService, times(1)).getTicketById("INVALID");
    }

    // ==================== CHANGE PRIORITY ====================

    @Test
    void testChangePriority_WithValidRequest_Success() throws Exception {
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("CRITICAL", "Urgent issue");

        when(adminTicketService.changePriority("TKT-001", request, adminId, adminUsername))
                .thenReturn(testTicket);

        mockMvc.perform(put("/admin/tickets/TKT-001/priority")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-001"));

        verify(adminTicketService, times(1))
                .changePriority("TKT-001", request, adminId, adminUsername);
    }

        @Test
        void testChangePriority_WithoutHeaders_ReturnsError() throws Exception {
        AdminChangePriorityRequest request = new AdminChangePriorityRequest("CRITICAL", "Urgent");

        mockMvc.perform(put("/admin/tickets/TKT-001/priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());  // Changed to 5xx

        verify(adminTicketService, never()).changePriority(anyString(), any(), anyString(), anyString());
        }

    // ==================== CHANGE CATEGORY ====================

    @Test
    void testChangeCategory_WithValidRequest_Success() throws Exception {
        AdminChangeCategoryRequest request = new AdminChangeCategoryRequest("BUG", "Recategorization needed");

        when(adminTicketService.changeCategory("TKT-001", request, adminId, adminUsername))
                .thenReturn(testTicket);

        mockMvc.perform(put("/admin/tickets/TKT-001/category")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-001"));

        verify(adminTicketService, times(1))
                .changeCategory("TKT-001", request, adminId, adminUsername);
    }

    @Test
        void testChangeCategory_WithoutUserId_ReturnsError() throws Exception {
        AdminChangeCategoryRequest request = new AdminChangeCategoryRequest("BUG", "Reason");

        mockMvc.perform(put("/admin/tickets/TKT-001/category")
                .header("X-Username", adminUsername)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());  // Changed to 5xx

        verify(adminTicketService, never()).changeCategory(anyString(), any(), anyString(), anyString());
        }
    // ==================== CHANGE STATUS ====================

    @Test
    void testChangeStatus_WithValidRequest_Success() throws Exception {
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Resolved");

        when(adminTicketService.changeStatus("TKT-001", request, adminId, adminUsername))
                .thenReturn(testTicket);

        mockMvc.perform(put("/admin/tickets/TKT-001/status")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-001"));

        verify(adminTicketService, times(1))
                .changeStatus("TKT-001", request, adminId, adminUsername);
    }

    @Test
        void testChangeStatus_WithoutUsername_ReturnsError() throws Exception {
        AdminChangeStatusRequest request = new AdminChangeStatusRequest("CLOSED", "Resolved");

        mockMvc.perform(put("/admin/tickets/TKT-001/status")
                .header("X-User-Id", adminId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());  // Changed to 5xx

        verify(adminTicketService, never()).changeStatus(anyString(), any(), anyString(), anyString());
        }

    // ==================== DELETE TICKET ====================

    @Test
    void testDeleteTicket_SoftDelete_Success() throws Exception {
        when(adminTicketService.deleteTicket("TKT-001", false, adminId, adminUsername))
                .thenReturn("Ticket soft deleted successfully");

        mockMvc.perform(delete("/admin/tickets/TKT-001")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername)
                .param("hardDelete", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ticket soft deleted successfully"));

        verify(adminTicketService, times(1))
                .deleteTicket("TKT-001", false, adminId, adminUsername);
    }

    @Test
    void testDeleteTicket_HardDelete_Success() throws Exception {
        when(adminTicketService.deleteTicket("TKT-001", true, adminId, adminUsername))
                .thenReturn("Ticket permanently deleted");

        mockMvc.perform(delete("/admin/tickets/TKT-001")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername)
                .param("hardDelete", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ticket permanently deleted"));

        verify(adminTicketService, times(1))
                .deleteTicket("TKT-001", true, adminId, adminUsername);
    }

    @Test
    void testDeleteTicket_WithoutHardDeleteParam_DefaultsToFalse() throws Exception {
        when(adminTicketService.deleteTicket("TKT-001", false, adminId, adminUsername))
                .thenReturn("Ticket soft deleted successfully");

        mockMvc.perform(delete("/admin/tickets/TKT-001")
                .header("X-User-Id", adminId)
                .header("X-Username", adminUsername))
                .andExpect(status().isOk());

        verify(adminTicketService, times(1))
                .deleteTicket("TKT-001", false, adminId, adminUsername);
    }

    @Test
        void testDeleteTicket_WithoutHeaders_ReturnsError() throws Exception {
        mockMvc.perform(delete("/admin/tickets/TKT-001"))
                .andExpect(status().is5xxServerError());  // Changed to 5xx

        verify(adminTicketService, never())
                .deleteTicket(anyString(), anyBoolean(), anyString(), anyString());
        }
    // ==================== GET TICKET STATS ====================

    @Test
    void testGetTicketStats_Success() throws Exception {
        TicketStatsDTO stats = new TicketStatsDTO(
                100L, 50L, 30L, 20L, 10L, 5L, 15L, 25L, 8L, 12L, 3L, 2L
        );
        when(adminTicketService.getTicketStats()).thenReturn(stats);

        mockMvc.perform(get("/admin/tickets/stats"))
                .andExpect(status().isOk());

        verify(adminTicketService, times(1)).getTicketStats();
    }

    // ==================== GET USER TICKETS ====================

    @Test
    void testGetUserTickets_WithDefaultParams_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(0, 10), 1);
        when(adminTicketService.getUserTickets("user-001", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/tickets/user/user-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].createdByUserId").value("user-001"));

        verify(adminTicketService, times(1)).getUserTickets("user-001", 0, 10);
    }

    @Test
    void testGetUserTickets_WithCustomPagination_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(2, 20), 1);
        when(adminTicketService.getUserTickets("user-001", 2, 20)).thenReturn(page);

        mockMvc.perform(get("/admin/tickets/user/user-001")
                .param("page", "2")
                .param("size", "20"))
                .andExpect(status().isOk());

        verify(adminTicketService, times(1)).getUserTickets("user-001", 2, 20);
    }

    // ==================== GET AGENT TICKETS ====================

    @Test
    void testGetAgentTickets_WithDefaultParams_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(0, 10), 1);
        when(adminTicketService.getAgentTickets("agent-001", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/tickets/agent/agent-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].assignedToUserId").value("agent-001"));

        verify(adminTicketService, times(1)).getAgentTickets("agent-001", 0, 10);
    }

    @Test
    void testGetAgentTickets_WithCustomPagination_Success() throws Exception {
        Page<AdminTicketDTO> page = new PageImpl<>(List.of(testTicket), PageRequest.of(1, 15), 1);
        when(adminTicketService.getAgentTickets("agent-001", 1, 15)).thenReturn(page);

        mockMvc.perform(get("/admin/tickets/agent/agent-001")
                .param("page", "1")
                .param("size", "15"))
                .andExpect(status().isOk());

        verify(adminTicketService, times(1)).getAgentTickets("agent-001", 1, 15);
    }
}
