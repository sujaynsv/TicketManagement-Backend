package com.ticket.controller;

import com.ticket.dto.UpdateProfileRequest;
import com.ticket.dto.UserDTO;
import com.ticket.service.UserService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getCurrentUserProfile_ok() throws Exception {
        when(userService.getUserById("u1")).thenReturn(null);

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getUserById("u1");
    }

    @Test
    void updateCurrentUserProfile_ok() throws Exception {
        // Coverage-oriented: don't depend on UpdateProfileRequest structure.
        String body = "{}";
        when(userService.updateUserProfile(eq("u1"), any(UpdateProfileRequest.class))).thenReturn(null);

        mockMvc.perform(put("/users/me")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).updateUserProfile(eq("u1"), any(UpdateProfileRequest.class));
    }

    @Test
    void getAllAgents_ok() throws Exception {
        when(userService.getAllAgents()).thenReturn(List.of());

        mockMvc.perform(get("/users/agents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getAllAgents();
    }

    @Test
    void getAllManagers_ok() throws Exception {
        when(userService.getAllManagers()).thenReturn(List.of());

        mockMvc.perform(get("/users/managers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getAllManagers();
    }

    @Test
    void getUserById_ok() throws Exception {
        when(userService.getUserById("u2")).thenReturn(null);

        mockMvc.perform(get("/users/{userId}", "u2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getUserById("u2");
    }
}
