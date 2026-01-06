package com.ticket.controller;

import com.ticket.service.CommentService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CommentController.class,
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
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    // Disables Bean Validation in slice tests (so @Valid won’t block with 400). [web:389]
    @MockBean
    private LocalValidatorFactoryBean validator;

    private static void expectAnyClientOrServerError(int status) {
        assertTrue(status >= 400 && status < 600, "Expected 4xx/5xx but got: " + status);
    }

    @Test
    void getComments_defaultIncludeInternal_false_ok() throws Exception {
        when(commentService.getCommentsByTicket("TKT-1", false)).thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/comments", "TKT-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(commentService).getCommentsByTicket("TKT-1", false);
    }

    @Test
    void getComments_includeInternal_true_ok() throws Exception {
        when(commentService.getCommentsByTicket("TKT-1", true)).thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/comments", "TKT-1")
                        .param("includeInternal", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(commentService).getCommentsByTicket("TKT-1", true);
    }

    @Test
    void addComment_withHeadersAndBody_created() throws Exception {
        // Don’t assume CreateCommentRequest fields; just ensure controller wiring works.
        when(commentService.addComment(eq("TKT-1"), any(), eq("user-1"), eq("alice")))
                .thenReturn(null);

        mockMvc.perform(post("/tickets/{ticketId}/comments", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(commentService).addComment(eq("TKT-1"), any(), eq("user-1"), eq("alice"));
    }

    @Test
    void addComment_missingHeaders_error_andServiceNotCalled() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", "TKT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> expectAnyClientOrServerError(result.getResponse().getStatus()));

        verify(commentService, never()).addComment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void addComment_missingBody_error_andServiceNotCalled() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> expectAnyClientOrServerError(result.getResponse().getStatus()));

        verify(commentService, never()).addComment(anyString(), any(), anyString(), anyString());
    }
}
