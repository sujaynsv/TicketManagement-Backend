package com.ticket.service;

import com.ticket.dto.UpdateProfileRequest;
import com.ticket.dto.UserDTO;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user(UUID id, UserRole role) {
        User u = new User();
        u.setUserId(id);
        u.setUsername("oldName");
        u.setEmail("old@mail.com");
        u.setFirstName("First");
        u.setLastName("Last");
        u.setRole(role);
        u.setIsActive(true);
        u.setCreatedAt(LocalDateTime.now().minusDays(5));
        u.setLastLogin(LocalDateTime.now().minusHours(2));
        return u;
    }

    @Test
    void getAllAgents_mapsEntitiesToDTOs() {
        User u1 = user(UUID.randomUUID(), UserRole.SUPPORT_AGENT);
        User u2 = user(UUID.randomUUID(), UserRole.SUPPORT_AGENT);

        when(userRepository.findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true))
                .thenReturn(List.of(u1, u2));

        List<UserDTO> result = userService.getAllAgents();

        assertEquals(2, result.size());
        assertEquals(u1.getUserId().toString(), result.get(0).userId());
        assertEquals("SUPPORT_AGENT", result.get(0).role());

        verify(userRepository).findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true);
    }

    @Test
    void getAllManagers_mapsEntitiesToDTOs() {
        User u1 = user(UUID.randomUUID(), UserRole.SUPPORT_MANAGER);

        when(userRepository.findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true))
                .thenReturn(List.of(u1));

        List<UserDTO> result = userService.getAllManagers();

        assertEquals(1, result.size());
        assertEquals("SUPPORT_MANAGER", result.get(0).role());

        verify(userRepository).findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true);
    }

    @Test
    void getUserById_found_returnsDTO() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.SUPPORT_AGENT);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        UserDTO dto = userService.getUserById(id.toString());

        assertEquals(id.toString(), dto.userId());
        assertEquals("oldName", dto.username());

        verify(userRepository).findById(id);
    }

    @Test
    void getUserById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserById(id.toString()));

        assertTrue(ex.getMessage().contains("User not found"));
        verify(userRepository).findById(id);
    }

    @Test
    void updateUserProfile_updatesNameOnly_whenEmailBlank() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.SUPPORT_AGENT);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("newName");
        req.setEmail("   "); // blank => should not update email / not check repo

        UserDTO dto = userService.updateUserProfile(id.toString(), req);

        assertEquals("newName", dto.username());
        assertEquals("old@mail.com", dto.email());

        verify(userRepository).findById(id);
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_updatesEmail_whenNotUsed() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.SUPPORT_AGENT);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName(null); // no name update
        req.setEmail("new@mail.com");

        UserDTO dto = userService.updateUserProfile(id.toString(), req);

        assertEquals("oldName", dto.username());
        assertEquals("new@mail.com", dto.email());

        verify(userRepository).findById(id);
        verify(userRepository).findByEmail("new@mail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_emailAlreadyUsed_throws() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.SUPPORT_AGENT);

        UUID otherId = UUID.randomUUID();
        User existing = user(otherId, UserRole.SUPPORT_AGENT);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.findByEmail("taken@mail.com")).thenReturn(Optional.of(existing));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("taken@mail.com");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateUserProfile(id.toString(), req));

        assertTrue(ex.getMessage().contains("Email already in use"));

        verify(userRepository).findById(id);
        verify(userRepository).findByEmail("taken@mail.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserProfile_userNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("x");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateUserProfile(id.toString(), req));

        assertTrue(ex.getMessage().contains("User not found"));
        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }
}
