package com.ticket.controller;

import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.service.AttachmentService;
import com.ticket.service.TicketService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TicketController.class,
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
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private AttachmentService attachmentService;

    // Prevent @Valid from failing controller tests (only affects validation, not JSON binding).
    @MockBean
    private LocalValidatorFactoryBean validator;

    private static TicketDTO sampleTicket(String ticketId, int attachmentCount) {
        LocalDateTime now = LocalDateTime.now();
        return new TicketDTO(
                ticketId,                 // ticketId
                "T-1",                    // ticketNumber
                "title",                  // title
                "desc",                   // description
                "OPEN",                   // status
                "TECHNICAL_ISSUE",        // category
                "HIGH",                   // priority
                "user-1",                 // createdByUserId
                "alice",                  // createdByUsername
                null,                     // assignedToUserId
                null,                     // assignedToUsername
                List.of("tag1"),          // tags
                0,                        // commentCount
                attachmentCount,          // attachmentCount
                now,                      // createdAt
                now,                      // updatedAt
                null,                     // assignedAt
                null,                     // resolvedAt
                null                      // closedAt
        );
    }

    @Test
    void health_ok() throws Exception {
        mockMvc.perform(get("/tickets/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ticket Service is running!"));
    }

    @Test
    void createTicket_noFiles_created() throws Exception {
        TicketDTO created = sampleTicket("TKT-1", 0);

        when(ticketService.createTicket(any(CreateTicketRequest.class), eq("user-1"), eq("alice")))
                .thenReturn(created);

        mockMvc.perform(multipart("/tickets")
                        .param("title", "My title")
                        .param("description", "My description")
                        .param("category", "TECHNICAL_ISSUE")
                        .param("tags", "tag1", "tag2")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketId").value("TKT-1"))
                .andExpect(jsonPath("$.attachmentCount").value(0));

        verify(ticketService).createTicket(any(CreateTicketRequest.class), eq("user-1"), eq("alice"));
        verifyNoInteractions(attachmentService);
        verify(ticketService, never()).updateAttachmentCount(anyString(), anyInt());
    }

    @Test
    void createTicket_withOneFile_createdAndUploads_andUpdatesAttachmentCount() throws Exception {
        TicketDTO created = sampleTicket("TKT-2", 0);

        when(ticketService.createTicket(any(CreateTicketRequest.class), eq("user-1"), eq("alice")))
                .thenReturn(created);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.pdf",
                "application/pdf",
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/tickets")
                        .file(file)
                        .param("title", "My title")
                        .param("description", "My description")
                        .param("category", "TECHNICAL_ISSUE")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value("TKT-2"))
                .andExpect(jsonPath("$.attachmentCount").value(1));

        verify(ticketService).createTicket(any(CreateTicketRequest.class), eq("user-1"), eq("alice"));
        verify(attachmentService).uploadAttachment(eq("TKT-2"), any(), eq("user-1"), eq("alice"));
        verify(ticketService).updateAttachmentCount("TKT-2", 1);
    }

    @Test
    void getTicketById_ok() throws Exception {
        when(ticketService.getTicketById("TKT-1")).thenReturn(sampleTicket("TKT-1", 0));

        mockMvc.perform(get("/tickets/{ticketId}", "TKT-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-1"));
    }

    @Test
    void getTicketByNumber_ok() throws Exception {
        when(ticketService.getTicketByNumber("T-1")).thenReturn(sampleTicket("TKT-1", 0));

        mockMvc.perform(get("/tickets/number/{ticketNumber}", "T-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value("T-1"));
    }

    @Test
    void updateTicket_ok() throws Exception {
        when(ticketService.updateTicket(eq("TKT-1"), any(), eq("user-1"), eq("alice")))
                .thenReturn(sampleTicket("TKT-1", 0));

        mockMvc.perform(put("/tickets/{ticketId}", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-1"));

        verify(ticketService).updateTicket(eq("TKT-1"), any(), eq("user-1"), eq("alice"));
    }

    @Test
    void changeStatus_ok() throws Exception {
        when(ticketService.changeStatus(eq("TKT-1"), any(), eq("user-1"), eq("alice")))
                .thenReturn(sampleTicket("TKT-1", 0));

        mockMvc.perform(patch("/tickets/{ticketId}/status", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ticketService).changeStatus(eq("TKT-1"), any(), eq("user-1"), eq("alice"));
    }

    @Test
    void getMyTickets_ok() throws Exception {
        when(ticketService.getMyTickets("user-1")).thenReturn(List.of(sampleTicket("TKT-1", 0)));

        mockMvc.perform(get("/tickets/my")
                        .header("X-User-Id", "user-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value("TKT-1"));

        verify(ticketService).getMyTickets("user-1");
    }

    @Test
    void getAssignedTickets_ok() throws Exception {
        when(ticketService.getAssignedTickets("user-1")).thenReturn(List.of());

        mockMvc.perform(get("/tickets/assigned")
                        .header("X-User-Id", "user-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(ticketService).getAssignedTickets("user-1");
    }

    @Test
    void getTicketsByStatus_ok() throws Exception {
        when(ticketService.getTicketsByStatus("OPEN")).thenReturn(List.of());

        mockMvc.perform(get("/tickets/status/{status}", "OPEN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(ticketService).getTicketsByStatus("OPEN");
    }

    @Test
    void getAllTickets_ok() throws Exception {
        when(ticketService.getAllTickets()).thenReturn(List.of());

        mockMvc.perform(get("/tickets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(ticketService).getAllTickets();
    }

    @Test
    void deleteTicket_ok() throws Exception {
        doNothing().when(ticketService).deleteTicket("TKT-1");

        mockMvc.perform(delete("/tickets/{ticketId}", "TKT-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ticket deleted successfully"));

        verify(ticketService).deleteTicket("TKT-1");
    }

    @Test
    void updateTicketPriority_ok() throws Exception {
        when(ticketService.updateTicketPriority(
                eq("TKT-1"),
                nullable(String.class),
                nullable(String.class),
                eq("mgr-1"),
                eq("manager")
        )).thenReturn(sampleTicket("TKT-1", 0));

        mockMvc.perform(patch("/tickets/{ticketId}/priority", "TKT-1")
                        .header("X-User-Id", "mgr-1")
                        .header("X-Username", "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("TKT-1"));

        verify(ticketService).updateTicketPriority(
                eq("TKT-1"),
                nullable(String.class),
                nullable(String.class),
                eq("mgr-1"),
                eq("manager")
        );
    }
}
