package com.ticket.controller;

import com.ticket.entity.TicketActivity;
import com.ticket.repository.TicketActivityRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TicketActivityController.class,
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
class TicketActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketActivityRepository ticketActivityRepository;

    @Test
    void getActivities_returnsListOfDtos_ok() throws Exception {
        String ticketId = "TKT-001";

        TicketActivity a1 = mock(TicketActivity.class);
        when(a1.getActivityId()).thenReturn("ACT-1");
        when(a1.getTicketId()).thenReturn(ticketId);
        when(a1.getActivityType()).thenReturn("STATUS_CHANGED");
        when(a1.getDescription()).thenReturn("OPEN -> ASSIGNED");
        when(a1.getPerformedByUserId()).thenReturn("admin-1");
        when(a1.getPerformedByUsername()).thenReturn("adminUser");
        when(a1.getOldValue()).thenReturn("OPEN");
        when(a1.getNewValue()).thenReturn("ASSIGNED");
        when(a1.getCreatedAt()).thenReturn(LocalDateTime.now());

        TicketActivity a2 = mock(TicketActivity.class);
        when(a2.getActivityId()).thenReturn("ACT-2");
        when(a2.getTicketId()).thenReturn(ticketId);
        when(a2.getActivityType()).thenReturn("PRIORITY_CHANGED");
        when(a2.getDescription()).thenReturn("null -> HIGH");
        when(a2.getPerformedByUserId()).thenReturn("admin-1");
        when(a2.getPerformedByUsername()).thenReturn("adminUser");
        when(a2.getOldValue()).thenReturn(null);
        when(a2.getNewValue()).thenReturn("HIGH");
        when(a2.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(ticketActivityRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
                .thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/tickets/{ticketId}/activities", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activityId").value("ACT-1"))
                .andExpect(jsonPath("$[0].ticketId").value(ticketId))
                .andExpect(jsonPath("$[0].activityType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[0].newValue").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].activityId").value("ACT-2"))
                .andExpect(jsonPath("$[1].activityType").value("PRIORITY_CHANGED"))
                .andExpect(jsonPath("$[1].newValue").value("HIGH"));

        verify(ticketActivityRepository, times(1))
                .findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    @Test
    void getActivities_emptyList_ok() throws Exception {
        String ticketId = "TKT-EMPTY";

        when(ticketActivityRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
                .thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/activities", ticketId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(ticketActivityRepository, times(1))
                .findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
}
