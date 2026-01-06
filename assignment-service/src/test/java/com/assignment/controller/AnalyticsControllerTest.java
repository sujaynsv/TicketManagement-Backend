package com.assignment.controller;

import com.assignment.service.AnalyticsService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnalyticsController.class,
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
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void getSystemOverview_returnsOk() throws Exception {
        when(analyticsService.getSystemOverview()).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getSystemOverview();
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getTicketAnalytics_defaultDays_uses7() throws Exception {
        when(analyticsService.getTicketAnalytics(7)).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/tickets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getTicketAnalytics(7);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getTicketAnalytics_customDays_passesParam() throws Exception {
        when(analyticsService.getTicketAnalytics(14)).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/tickets")
                        .param("days", "14")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getTicketAnalytics(14);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getAgentPerformance_returnsOk() throws Exception {
        when(analyticsService.getAgentPerformance()).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/agents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getAgentPerformance();
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getSlaReport_returnsOk() throws Exception {
        when(analyticsService.getSlaReport()).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/sla")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getSlaReport();
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getCategoryBreakdown_returnsOk() throws Exception {
        when(analyticsService.getCategoryBreakdown()).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getCategoryBreakdown();
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getTrends_defaultParams_daily_30() throws Exception {
        when(analyticsService.getTrends("daily", 30)).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/trends")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getTrends("daily", 30);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    void getTrends_customParams_passedToService() throws Exception {
        when(analyticsService.getTrends("weekly", 60)).thenReturn(null);

        mockMvc.perform(get("/admin/analytics/trends")
                        .param("period", "weekly")
                        .param("days", "60")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(analyticsService).getTrends("weekly", 60);
        verifyNoMoreInteractions(analyticsService);
    }
}
