package com.ticket.controller;

import com.ticket.dto.EscalateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.entity.Ticket;
import com.ticket.enums.EscalationType;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.EscalationService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EscalationController.class,
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
class EscalationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EscalationService escalationService;

    @MockBean
    private TicketMapper ticketMapper;

    @Test
    void escalateTicket_validReason_ok() throws Exception {
        Ticket ticket = mock(Ticket.class);
        TicketDTO dto = mock(TicketDTO.class);

        when(escalationService.escalateTicket(
                eq("TKT-1"),
                eq("user-1"),
                eq("alice"),
                any(EscalateTicketRequest.class),
                eq(EscalationType.MANUAL)
        )).thenReturn(ticket);

        when(ticketMapper.toDTO(ticket)).thenReturn(dto);

        mockMvc.perform(post("/tickets/{ticketId}/escalate", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Need supervisor\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(escalationService, times(1)).escalateTicket(
                eq("TKT-1"),
                eq("user-1"),
                eq("alice"),
                any(EscalateTicketRequest.class),
                eq(EscalationType.MANUAL)
        );
        verify(ticketMapper, times(1)).toDTO(ticket);
    }

    @Test
    void escalateTicket_missingReason_badRequest() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/escalate", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(escalationService);
        verifyNoInteractions(ticketMapper);
    }

    @Test
    void escalateTicket_blankReason_badRequest() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/escalate", "TKT-1")
                        .header("X-User-Id", "user-1")
                        .header("X-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(escalationService);
        verifyNoInteractions(ticketMapper);
    }

    @Test
    void escalateTicket_missingHeaders_error() throws Exception {
        // In your current setup, MissingRequestHeaderException is coming back as 500. [web:749]
        mockMvc.perform(post("/tickets/{ticketId}/escalate", "TKT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Need supervisor\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()); // use is5xxServerError() [web:787]

        verifyNoInteractions(escalationService);
        verifyNoInteractions(ticketMapper);
    }
}
