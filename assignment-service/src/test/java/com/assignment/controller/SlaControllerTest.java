package com.assignment.controller;

import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaTrackingRepository;
import com.assignment.service.SlaService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SlaController.class,
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
class SlaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SlaService slaService;

    @MockBean
    private SlaTrackingRepository slaTrackingRepository;

    private static SlaTracking sampleTracking(String trackingId, String ticketId, SlaStatus status) {
        SlaTracking t = new SlaTracking();
        t.setTrackingId(trackingId);
        t.setTicketId(ticketId);
        t.setTicketNumber("T-100");
        t.setPriority("HIGH");
        t.setCategory("AUTH");
        t.setResponseDueAt(LocalDateTime.now().plusMinutes(30));
        t.setResolutionDueAt(LocalDateTime.now().plusHours(4));
        t.setFirstResponseAt(null);
        t.setResponseBreached(false);

        // setter expects Integer in your entity
        t.setResponseTimeMinutes(10);

        t.setResolvedAt(null);
        t.setResolutionBreached(false);
        t.setResolutionTimeHours(new BigDecimal("1.5"));
        t.setSlaStatus(status);
        return t;
    }

    @Test
    void getSlaTracking_found_returnsDto() throws Exception {
        SlaTracking tracking = sampleTracking("trk1", "ticket1", SlaStatus.WARNING);

        when(slaService.getSlaTracking("ticket1")).thenReturn(Optional.of(tracking));
        when(slaService.calculateTimeRemaining(tracking)).thenReturn("2h");

        mockMvc.perform(get("/sla/tickets/{ticketId}", "ticket1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.trackingId").value("trk1"))
                .andExpect(jsonPath("$.ticketId").value("ticket1"))
                .andExpect(jsonPath("$.ticketNumber").value("T-100"))
                .andExpect(jsonPath("$.slaStatus").value("WARNING"))
                .andExpect(jsonPath("$.timeRemaining").value("2h"));

        verify(slaService).getSlaTracking("ticket1");
        verify(slaService).calculateTimeRemaining(tracking);
        verifyNoInteractions(slaTrackingRepository);
    }

    @Test
    void getSlaTracking_notFound_throwsServletException() {
        when(slaService.getSlaTracking("missing")).thenReturn(Optional.empty());

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/sla/tickets/{ticketId}", "missing")
                        .accept(MediaType.APPLICATION_JSON))
        );

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("SLA tracking not found for ticket"));

        verify(slaService).getSlaTracking("missing");
    }

    @Test
    void getBreachedSlas_returnsList() throws Exception {
        SlaTracking t1 = sampleTracking("b1", "ticket1", SlaStatus.BREACHED);
        SlaTracking t2 = sampleTracking("b2", "ticket2", SlaStatus.BREACHED);

        when(slaTrackingRepository.findBySlaStatus(SlaStatus.BREACHED)).thenReturn(List.of(t1, t2));
        when(slaService.calculateTimeRemaining(any(SlaTracking.class))).thenReturn("NA");

        mockMvc.perform(get("/sla/breached").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slaStatus").value("BREACHED"))
                .andExpect(jsonPath("$[1].slaStatus").value("BREACHED"));

        verify(slaTrackingRepository).findBySlaStatus(SlaStatus.BREACHED);
        verify(slaService, times(2)).calculateTimeRemaining(any(SlaTracking.class));
    }

    @Test
    void getActiveSlas_returnsList() throws Exception {
        SlaTracking t1 = sampleTracking("a1", "ticket1", SlaStatus.ON_TIME);
        SlaTracking t2 = sampleTracking("a2", "ticket2", SlaStatus.WARNING);

        when(slaTrackingRepository.findByResolvedAtIsNull()).thenReturn(List.of(t1, t2));
        when(slaService.calculateTimeRemaining(any(SlaTracking.class))).thenReturn("1h");

        mockMvc.perform(get("/sla/active").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trackingId").value("a1"))
                .andExpect(jsonPath("$[1].trackingId").value("a2"));

        verify(slaTrackingRepository).findByResolvedAtIsNull();
        verify(slaService, times(2)).calculateTimeRemaining(any(SlaTracking.class));
    }

    @Test
    void getSlaWarnings_returnsList() throws Exception {
        SlaTracking t1 = sampleTracking("w1", "ticket1", SlaStatus.WARNING);

        when(slaTrackingRepository.findBySlaStatus(SlaStatus.WARNING)).thenReturn(List.of(t1));
        when(slaService.calculateTimeRemaining(any(SlaTracking.class))).thenReturn("30m");

        mockMvc.perform(get("/sla/warnings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trackingId").value("w1"))
                .andExpect(jsonPath("$[0].slaStatus").value("WARNING"))
                .andExpect(jsonPath("$[0].timeRemaining").value("30m"));

        verify(slaTrackingRepository).findBySlaStatus(SlaStatus.WARNING);
        verify(slaService).calculateTimeRemaining(any(SlaTracking.class));
    }
}
