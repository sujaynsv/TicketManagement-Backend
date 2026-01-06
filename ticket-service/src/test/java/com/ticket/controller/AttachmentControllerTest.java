package com.ticket.controller;

import com.ticket.dto.AttachmentDTO;
import com.ticket.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AttachmentController.class,
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
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    private AttachmentDTO testAttachment;
    private final String ticketId = "TKT-001";
    private final String userId = "user-001";
    private final String username = "testuser";
    private final String attachmentId = "ATT-001";

    @BeforeEach
    void setUp() {
        testAttachment = new AttachmentDTO(
                attachmentId,
                ticketId,
                "test-document.pdf",
                "test-document.pdf",
                "application/pdf",
                12345L,
                "https://s3.amazonaws.com/bucket/test-document.pdf",
                userId,
                username,
                LocalDateTime.now()
        );
    }

    @Test
    void testGetAttachments_WithExistingAttachments_Success() throws Exception {
        when(attachmentService.getAttachmentsByTicket(ticketId)).thenReturn(List.of(testAttachment));

        mockMvc.perform(get("/tickets/{ticketId}/attachments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(attachmentService, times(1)).getAttachmentsByTicket(ticketId);
    }

    @Test
    void testGetAttachments_WithNoAttachments_ReturnsEmptyList() throws Exception {
        when(attachmentService.getAttachmentsByTicket(ticketId)).thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/attachments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(attachmentService, times(1)).getAttachmentsByTicket(ticketId);
    }
}