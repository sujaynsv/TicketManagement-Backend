package com.ticket.controller;

import com.ticket.dto.LoginRequest;
import com.ticket.dto.RegisterRequest;
import com.ticket.service.AuthService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // Disables @Valid failures in @WebMvcTest when left unconfigured
    @MockBean
    private LocalValidatorFactoryBean validator;

    private static final String VALID_LOGIN_JSON =
            "{\"username\":\"john\",\"password\":\"pass123\"}";

    private static final String VALID_REGISTER_JSON =
            "{\"username\":\"john\",\"email\":\"john@mail.com\",\"password\":\"pass123\",\"firstName\":\"F\",\"lastName\":\"L\",\"roleName\":\"SUPPORT_AGENT\"}";

    @Test
    void login_success_returns200() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void login_serviceThrows_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void register_success_returns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_serviceThrows_returns400() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new RuntimeException("bad"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void health_returns200_andMessage() throws Exception {
        mockMvc.perform(get("/auth/health").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth Service is running")));

        verifyNoInteractions(authService);
    }

    @Test
    void validateToken_true_returns200_validTrue() throws Exception {
        when(authService.validateTokenWithVersion("tok")).thenReturn(true);

        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));

        verify(authService).validateTokenWithVersion("tok");
    }

    @Test
    void validateToken_false_returns200_validFalse() throws Exception {
        when(authService.validateTokenWithVersion("tok")).thenReturn(false);

        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));

        verify(authService).validateTokenWithVersion("tok");
    }

    @Test
    void validateToken_serviceThrows_returns401() throws Exception {
        when(authService.validateTokenWithVersion(anyString())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Token validation failed")));

        verify(authService).validateTokenWithVersion("tok");
    }

    @Test
    void logout_success_returns200() throws Exception {
        when(authService.logout("u1")).thenReturn(null);

        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(authService).logout("u1");
    }

    @Test
    void logout_serviceThrows_returns400_withErrorBody() throws Exception {
        when(authService.logout("u1")).thenThrow(new RuntimeException("fail"));

        mockMvc.perform(post("/auth/logout")
                        .header("X-User-Id", "u1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Logout Failed"))
                .andExpect(jsonPath("$.message").value("fail"))
                .andExpect(jsonPath("$.path").value("/auth/logout"));

        verify(authService).logout("u1");
    }
}
