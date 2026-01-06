package com.ticket.controller;

import com.ticket.dto.*;
import com.ticket.service.AdminUserService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminUserController.class,
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
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    // bypass @Valid in controller inputs
    @MockBean
    private LocalValidatorFactoryBean validator;

    @Test
    void getAllUsers_defaults_ok() throws Exception {
        when(adminUserService.getAllUsers(0, 10, null, null, null)).thenReturn(null);

        mockMvc.perform(get("/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getAllUsers(0, 10, null, null, null);
    }

    @Test
    void getAllUsers_withFilters_ok() throws Exception {
        when(adminUserService.getAllUsers(1, 5, "SUPPORT_AGENT", true, "john")).thenReturn(null);

        mockMvc.perform(get("/admin/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("role", "SUPPORT_AGENT")
                        .param("isActive", "true")
                        .param("search", "john")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getAllUsers(1, 5, "SUPPORT_AGENT", true, "john");
    }

    @Test
    void getUserById_ok() throws Exception {
        when(adminUserService.getUserById("u1")).thenReturn(null);

        mockMvc.perform(get("/admin/users/{userId}", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getUserById("u1");
    }

    @Test
    void createUser_returns201() throws Exception {
        when(adminUserService.createUser(any(CreateUserRequest.class))).thenReturn(null);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(adminUserService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void updateUser_ok() throws Exception {
        when(adminUserService.updateUser(eq("u1"), any(UpdateUserRequest.class))).thenReturn(null);

        mockMvc.perform(put("/admin/users/{userId}", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).updateUser(eq("u1"), any(UpdateUserRequest.class));
    }

    @Test
    void changeUserRole_ok() throws Exception {
        when(adminUserService.changeUserRole(eq("u1"), anyString())).thenReturn(null);

        mockMvc.perform(put("/admin/users/{userId}/role", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).changeUserRole(eq("u1"), eq("ADMIN"));
    }

    @Test
    void activateUser_ok() throws Exception {
        when(adminUserService.activateUser("u1")).thenReturn(null);

        mockMvc.perform(put("/admin/users/{userId}/activate", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).activateUser("u1");
    }

    @Test
    void deactivateUser_ok() throws Exception {
        when(adminUserService.deactivateUser("u1")).thenReturn(null);

        mockMvc.perform(put("/admin/users/{userId}/deactivate", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).deactivateUser("u1");
    }

    @Test
    void assignManager_ok() throws Exception {
        when(adminUserService.assignManager(eq("u1"), anyString())).thenReturn(null);

        mockMvc.perform(put("/admin/users/{userId}/manager", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":\"m1\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).assignManager("u1", "m1");
    }

    @Test
    void resetPassword_ok_returnsMessage() throws Exception {
        when(adminUserService.resetPassword("u1", "newPass")).thenReturn("Password reset successfully");

        mockMvc.perform(put("/admin/users/{userId}/reset-password", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPass\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(adminUserService).resetPassword("u1", "newPass");
    }

    @Test
    void getUserStats_ok() throws Exception {
        when(adminUserService.getUserStats()).thenReturn(null);

        mockMvc.perform(get("/admin/users/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getUserStats();
    }

    @Test
    void getAllAgents_ok() throws Exception {
        when(adminUserService.getAllAgents()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users/agents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getAllAgents();
    }

    @Test
    void getAllManagers_ok() throws Exception {
        when(adminUserService.getAllManagers()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users/managers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adminUserService).getAllManagers();
    }
}
