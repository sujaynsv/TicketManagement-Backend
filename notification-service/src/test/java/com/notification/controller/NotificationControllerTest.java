package com.notification.controller;

import com.notification.dto.NotificationDTO;
import com.notification.service.NotificationService;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = NotificationController.class,
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
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private NotificationDTO sampleDto(String id, String userId) {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationId(id);
        dto.setUserId(userId);
        dto.setSubject("Test Subject");
        dto.setMessage("Test Message");
        return dto;
    }

    @Test
    void getUserNotifications_returnsList() throws Exception {
        when(notificationService.getUserNotifications("u1"))
                .thenReturn(List.of(sampleDto("n1", "u1"), sampleDto("n2", "u1")));

        mockMvc.perform(get("/notifications/users/{userId}", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].notificationId", is("n1")));

        verify(notificationService).getUserNotifications("u1");
    }

    @Test
    void getUnreadNotifications_returnsList() throws Exception {
        when(notificationService.getUnreadNotifications("u1"))
                .thenReturn(List.of(sampleDto("n1", "u1")));

        mockMvc.perform(get("/notifications/users/{userId}/unread", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notificationId", is("n1")));

        verify(notificationService).getUnreadNotifications("u1");
    }

    @Test
    void getUnreadCount_returnsMap() throws Exception {
        when(notificationService.getUnreadCount("u1")).thenReturn(5L);

        mockMvc.perform(get("/notifications/users/{userId}/unread/count", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("u1")))
                // Note: jsonPath values are often coerced to Integer; 
                // for Long, use .value(is(5), Long.class) if needed [web:665]
                .andExpect(jsonPath("$.unreadCount").value(5));

        verify(notificationService).getUnreadCount("u1");
    }

    @Test
    void markAsRead_returnsConfirmation() throws Exception {
        // notificationService.markAsRead is void, so nothing to stub

        mockMvc.perform(patch("/notifications/{notificationId}/read", "n1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Notification marked as read")))
                .andExpect(jsonPath("$.notificationId", is("n1")));

        verify(notificationService).markAsRead("n1");
    }

    @Test
    void markAllAsRead_returnsConfirmation() throws Exception {
        // notificationService.markAllAsRead is void

        mockMvc.perform(patch("/notifications/users/{userId}/read-all", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("All notifications marked as read")))
                .andExpect(jsonPath("$.userId", is("u1")));

        verify(notificationService).markAllAsRead("u1");
    }
}
