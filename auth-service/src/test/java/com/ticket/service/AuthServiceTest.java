package com.ticket.service;

import com.ticket.dto.LoginRequest;
import com.ticket.dto.LoginResponse;
import com.ticket.dto.LogoutResponse;
import com.ticket.dto.RegisterRequest;
import com.ticket.dto.RegisterResponse;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.exception.AccountDeactivatedException;
import com.ticket.exception.BadCredentialsException;
import com.ticket.repository.UserRepository;
import com.ticket.security.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User baseUser(UUID id) {
        User u = new User();
        u.setUserId(id);
        u.setUsername("john");
        u.setEmail("john@mail.com");
        u.setRole(UserRole.SUPPORT_AGENT);
        u.setIsActive(true);
        u.setTokenVersion(0);
        u.setPasswordHash(new BCryptPasswordEncoder(12).encode("pass123"));
        return u;
    }

    // -------- login() --------

    @Test
    void login_success_returnsResponse_andSavesLastLogin() {
        User u = baseUser(UUID.randomUUID());
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(u));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("token-123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse resp = authService.login(new LoginRequest("john", "pass123"),response);

        assertNotNull(resp);
        verify(userRepository).findByUsername("john");

        // ensure lastLogin updated + saved
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastLogin());

        verify(jwtUtil).generateToken(
                eq(u.getUserId().toString()),
                eq("john"),
                eq("john@mail.com"),
                eq(u.getRole().name()),
                eq(u.getTokenVersion())
        );
    }

    @Test
    void login_invalidUsername_throwsBadCredentials() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("john", "pass123"),response));

        verify(userRepository).findByUsername("john");
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any(), anyInt());
    }

    @Test
    void login_inactiveUser_throwsAccountDeactivated() {
        User u = baseUser(UUID.randomUUID());
        u.setIsActive(false);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(u));

        assertThrows(AccountDeactivatedException.class,
                () -> authService.login(new LoginRequest("john", "pass123"),response));

        verify(userRepository).findByUsername("john");
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any(), anyInt());
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        User u = baseUser(UUID.randomUUID());

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(u));
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("john", "wrong"),response));

        verify(userRepository).findByUsername("john");
        verify(userRepository, never()).save(any());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any(), anyInt());
    }

    // -------- register() --------

    @Test
    void register_usernameExists_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                authService.register(new RegisterRequest(
                        "john", "john@mail.com", "pass123", "First", "Last", "SUPPORT_AGENT"
                ))
        );

        verify(userRepository).existsByUsername("john");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailExists_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                authService.register(new RegisterRequest(
                        "john", "john@mail.com", "pass123", "First", "Last", "SUPPORT_AGENT"
                ))
        );

        verify(userRepository).existsByUsername("john");
        verify(userRepository).existsByEmail("john@mail.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_success_savesUser() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse resp = authService.register(new RegisterRequest(
                "john", "john@mail.com", "pass123", "First", "Last", null
        ));

        assertNotNull(resp);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("john", saved.getUsername());
        assertEquals("john@mail.com", saved.getEmail());
        assertNotNull(saved.getPasswordHash());
        assertNotEquals("pass123", saved.getPasswordHash());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(Boolean.TRUE.equals(saved.getIsActive()));
    }

    // -------- logout() --------

    @Test
    void logout_success_incrementsTokenVersion_andSaves() {
        UUID id = UUID.randomUUID();
        User u = baseUser(id);
        u.setTokenVersion(7);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LogoutResponse resp = authService.logout(id.toString());

        assertNotNull(resp);
        assertEquals(8, u.getTokenVersion());
        verify(userRepository).save(u);
    }

    @Test
    void logout_userNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.logout(id.toString()));
        verify(userRepository, never()).save(any());
    }

    // -------- validateTokenWithVersion() --------

    @Test
    void validateTokenWithVersion_expiredToken_returnsFalse() {
        when(jwtUtil.extractUserId("t")).thenReturn(UUID.randomUUID().toString());
        when(jwtUtil.isTokenExpired("t")).thenReturn(true);

        assertFalse(authService.validateTokenWithVersion("t"));

        verify(jwtUtil).extractUserId("t");
        verify(jwtUtil).isTokenExpired("t");
        verify(jwtUtil, never()).extractTokenVersion(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void validateTokenWithVersion_userNotFound_returnsFalse() {
        UUID id = UUID.randomUUID();

        when(jwtUtil.extractUserId("t")).thenReturn(id.toString());
        when(jwtUtil.isTokenExpired("t")).thenReturn(false);
        when(jwtUtil.extractTokenVersion("t")).thenReturn(1);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertFalse(authService.validateTokenWithVersion("t"));
    }

    @Test
    void validateTokenWithVersion_versionMismatch_returnsFalse() {
        UUID id = UUID.randomUUID();
        User u = baseUser(id);
        u.setTokenVersion(5);

        when(jwtUtil.extractUserId("t")).thenReturn(id.toString());
        when(jwtUtil.isTokenExpired("t")).thenReturn(false);
        when(jwtUtil.extractTokenVersion("t")).thenReturn(4);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        assertFalse(authService.validateTokenWithVersion("t"));
    }

    @Test
    void validateTokenWithVersion_success_returnsTrue() {
        UUID id = UUID.randomUUID();
        User u = baseUser(id);
        u.setTokenVersion(2);

        when(jwtUtil.extractUserId("t")).thenReturn(id.toString());
        when(jwtUtil.isTokenExpired("t")).thenReturn(false);
        when(jwtUtil.extractTokenVersion("t")).thenReturn(2);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        assertTrue(authService.validateTokenWithVersion("t"));
    }

    @Test
    void validateTokenWithVersion_exception_returnsFalse() {
        when(jwtUtil.extractUserId("t")).thenThrow(new RuntimeException("boom"));

        assertFalse(authService.validateTokenWithVersion("t"));
    }
}
