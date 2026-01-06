package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.exception.EmailAlreadyExistsException;
import com.ticket.exception.ManagerAssignmentException;
import com.ticket.exception.UsernameAlreadyExistsException;
import com.ticket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    // --- helpers -------------------------------------------------------------

    private User user(UUID id, UserRole role, boolean active) {
        User u = new User();
        u.setUserId(id);
        u.setUsername("u-" + id.toString().substring(0, 6));
        u.setEmail("e-" + id.toString().substring(0, 6) + "@mail.com");
        u.setFirstName("First");
        u.setLastName("Last");
        u.setRole(role);
        u.setIsActive(active);
        u.setCreatedAt(LocalDateTime.now().minusDays(1));
        u.setUpdatedAt(LocalDateTime.now().minusHours(3));
        u.setLastLogin(LocalDateTime.now().minusHours(5));
        u.setTokenVersion(0);
        u.setPasswordHash("hash");
        return u;
    }

    /**
     * Creates a record (or any class) instance by matching constructor parameter count.
     * For records, component order is guaranteed by getRecordComponents(). [web:509]
     *
     * If your DTO is a record (high chance), this avoids guessing the exact canonical ctor order.
     */
    @SuppressWarnings("unchecked")
    private static <T> T newRecordLike(Class<T> type, Map<String, Object> valuesByName) {
        try {
            if (type.isRecord()) {
                var comps = type.getRecordComponents(); // order matches declaration [web:509]
                Class<?>[] paramTypes = Arrays.stream(comps).map(rc -> rc.getType()).toArray(Class[]::new);
                Object[] args = Arrays.stream(comps).map(rc -> valuesByName.get(rc.getName())).toArray();
                Constructor<T> ctor = type.getDeclaredConstructor(paramTypes);
                ctor.setAccessible(true);
                return ctor.newInstance(args);
            }

            // Fallback: try first public constructor with same arity as provided values
            for (Constructor<?> c : type.getDeclaredConstructors()) {
                if (c.getParameterCount() == valuesByName.size()) {
                    c.setAccessible(true);
                    return (T) c.newInstance(valuesByName.values().toArray());
                }
            }
            throw new RuntimeException("No suitable constructor for " + type.getName());
        } catch (Exception e) {
            throw new RuntimeException("Failed creating " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    // --- getAllUsers() -------------------------------------------------------

    @Test
    void getAllUsers_noFilters_callsFindAllWithPageable() {
        User u1 = user(UUID.randomUUID(), UserRole.END_USER, true);
        Page<User> page = new PageImpl<>(List.of(u1));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserDTO> result = adminUserService.getAllUsers(0, 10, null, null, null);

        assertEquals(1, result.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertEquals(0, used.getPageNumber());
        assertEquals(10, used.getPageSize());
        // Should sort by createdAt desc
        Sort.Order order = used.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getAllUsers_withRoleActiveSearch_callsFindAll() {
        Page<User> page = new PageImpl<>(List.of(user(UUID.randomUUID(), UserRole.SUPPORT_AGENT, true)));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AdminUserDTO> result = adminUserService.getAllUsers(1, 5, "SUPPORT_AGENT", true, "abc");

        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // --- getUserById() -------------------------------------------------------

    @Test
    void getUserById_found_returnsDto() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(user(id, UserRole.ADMIN, true)));

        AdminUserDTO dto = adminUserService.getUserById(id.toString());

        assertNotNull(dto);
        verify(userRepository).findById(id);
    }

    @Test
    void getUserById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminUserService.getUserById(id.toString()));
        assertTrue(ex.getMessage().contains("User not found"));

        verify(userRepository).findById(id);
    }

    // --- createUser() : username/email validation ----------------------------

    @Test
    void createUser_usernameExists_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "john");
            put("email", "john@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "END_USER");
            put("managerId", null);
        }});

        assertThrows(UsernameAlreadyExistsException.class, () -> adminUserService.createUser(req));
        verify(userRepository).existsByUsername("john");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_emailExists_throws() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@mail.com")).thenReturn(true);

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "john");
            put("email", "john@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "END_USER");
            put("managerId", null);
        }});

        assertThrows(EmailAlreadyExistsException.class, () -> adminUserService.createUser(req));
        verify(userRepository).existsByUsername("john");
        verify(userRepository).existsByEmail("john@mail.com");
        verify(userRepository, never()).save(any());
    }

    // --- createUser() : manager assignment rules -----------------------------

    @Test
    void createUser_adminWithManagerId_throwsManagerAssignmentException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "john");
            put("email", "john@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "ADMIN");
            put("managerId", UUID.randomUUID().toString());
        }});

        assertThrows(ManagerAssignmentException.class, () -> adminUserService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_agentWithoutManager_throwsManagerAssignmentException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "agent");
            put("email", "agent@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "SUPPORT_AGENT");
            put("managerId", "   "); // blank
        }});

        assertThrows(ManagerAssignmentException.class, () -> adminUserService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_managerNotFound_throws() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        String managerId = UUID.randomUUID().toString();
        when(userRepository.findById(UUID.fromString(managerId))).thenReturn(Optional.empty());

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "agent");
            put("email", "agent@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "SUPPORT_AGENT");
            put("managerId", managerId);
        }});

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminUserService.createUser(req));
        assertTrue(ex.getMessage().contains("Manager not found"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_managerWrongRole_throwsManagerAssignmentException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        String managerId = UUID.randomUUID().toString();
        User notManager = user(UUID.fromString(managerId), UserRole.END_USER, true);
        when(userRepository.findById(UUID.fromString(managerId))).thenReturn(Optional.of(notManager));

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "agent");
            put("email", "agent@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "SUPPORT_AGENT");
            put("managerId", managerId);
        }});

        assertThrows(ManagerAssignmentException.class, () -> adminUserService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_managerInactive_throwsManagerAssignmentException() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        String managerId = UUID.randomUUID().toString();
        User inactiveMgr = user(UUID.fromString(managerId), UserRole.SUPPORT_MANAGER, false);
        when(userRepository.findById(UUID.fromString(managerId))).thenReturn(Optional.of(inactiveMgr));

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "agent");
            put("email", "agent@mail.com");
            put("password", "p");
            put("firstName", "F");
            put("lastName", "L");
            put("role", "SUPPORT_AGENT");
            put("managerId", managerId);
        }});

        assertThrows(ManagerAssignmentException.class, () -> adminUserService.createUser(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_success_savesUser_andSetsManager() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        String managerId = UUID.randomUUID().toString();
        User mgr = user(UUID.fromString(managerId), UserRole.SUPPORT_MANAGER, true);
        mgr.setFirstName("M");
        mgr.setLastName("G");
        when(userRepository.findById(UUID.fromString(managerId))).thenReturn(Optional.of(mgr));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest req = newRecordLike(CreateUserRequest.class, new LinkedHashMap<>() {{
            put("username", "agent");
            put("email", "agent@mail.com");
            put("password", "plain");
            put("firstName", "A");
            put("lastName", "B");
            put("role", "SUPPORT_AGENT");
            put("managerId", managerId);
        }});

        AdminUserDTO dto = adminUserService.createUser(req);

        assertNotNull(dto);
        verify(userRepository).save(any(User.class));
    }

    // --- updateUser() --------------------------------------------------------

    @Test
    void updateUser_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UpdateUserRequest req = newRecordLike(UpdateUserRequest.class, new LinkedHashMap<>() {{
            put("email", "new@mail.com");
            put("firstName", "N");
            put("lastName", "L");
        }});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminUserService.updateUser(id.toString(), req));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void updateUser_emailChangedToExisting_throws() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, true);
        u.setEmail("old@mail.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmail("taken@mail.com")).thenReturn(true);

        UpdateUserRequest req = newRecordLike(UpdateUserRequest.class, new LinkedHashMap<>() {{
            put("email", "taken@mail.com");
            put("firstName", null);
            put("lastName", null);
        }});

        assertThrows(EmailAlreadyExistsException.class, () -> adminUserService.updateUser(id.toString(), req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_success_updatesFields_andSaves() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, true);
        u.setEmail("old@mail.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest req = newRecordLike(UpdateUserRequest.class, new LinkedHashMap<>() {{
            put("email", "new@mail.com");
            put("firstName", "NewF");
            put("lastName", "NewL");
        }});

        AdminUserDTO dto = adminUserService.updateUser(id.toString(), req);

        assertNotNull(dto);
        assertEquals("new@mail.com", u.getEmail());
        assertEquals("NewF", u.getFirstName());
        assertEquals("NewL", u.getLastName());
        verify(userRepository).save(u);
    }

    // --- changeUserRole / activate / deactivate -----------------------------

    @Test
    void changeUserRole_success_saves() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, true);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDTO dto = adminUserService.changeUserRole(id.toString(), "ADMIN");

        assertNotNull(dto);
        assertEquals(UserRole.ADMIN, u.getRole());
        verify(userRepository).save(u);
    }

    @Test
    void activateUser_success_setsActiveTrue() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, false);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        adminUserService.activateUser(id.toString());

        assertTrue(Boolean.TRUE.equals(u.getIsActive()));
        verify(userRepository).save(u);
    }

    @Test
    void deactivateUser_success_setsActiveFalse() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, true);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        adminUserService.deactivateUser(id.toString());

        assertTrue(Boolean.FALSE.equals(u.getIsActive()));
        verify(userRepository).save(u);
    }

    // --- assignManager() -----------------------------------------------------

    @Test
    void assignManager_managerWrongRole_throws() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        User u = user(userId, UserRole.SUPPORT_AGENT, true);
        User notMgr = user(managerId, UserRole.END_USER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(notMgr));

        assertThrows(ManagerAssignmentException.class,
                () -> adminUserService.assignManager(userId.toString(), managerId.toString()));

        verify(userRepository, never()).save(any());
    }

    @Test
    void assignManager_success_setsManager_andSaves() {
        UUID userId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        User u = user(userId, UserRole.SUPPORT_AGENT, true);
        User mgr = user(managerId, UserRole.ADMIN, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(mgr));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDTO dto = adminUserService.assignManager(userId.toString(), managerId.toString());

        assertNotNull(dto);
        assertEquals(mgr, u.getManager());
        verify(userRepository).save(u);
    }

    // --- resetPassword() -----------------------------------------------------

    @Test
    void resetPassword_success_returnsMessage_andSaves() {
        UUID id = UUID.randomUUID();
        User u = user(id, UserRole.END_USER, true);

        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        String msg = adminUserService.resetPassword(id.toString(), "newPass");

        assertEquals("Password reset successfully", msg);
        verify(userRepository).save(u);
        assertNotNull(u.getPasswordHash());
        assertNotEquals("newPass", u.getPasswordHash());
    }

    // --- getUserStats() ------------------------------------------------------

    @Test
    void getUserStats_returnsCounts() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActive(true)).thenReturn(80L);
        when(userRepository.countByIsActive(false)).thenReturn(20L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(UserRole.SUPPORT_MANAGER)).thenReturn(5L);
        when(userRepository.countByRole(UserRole.SUPPORT_AGENT)).thenReturn(10L);
        when(userRepository.countByRole(UserRole.END_USER)).thenReturn(84L);

        UserStatsDTO stats = adminUserService.getUserStats();

        assertNotNull(stats);
        verify(userRepository).count();
        verify(userRepository).countByRole(UserRole.END_USER);
    }

    // --- getAllAgents / getAllManagers --------------------------------------

    @Test
    void getAllAgents_mapsToUserDTO() {
        User u = user(UUID.randomUUID(), UserRole.SUPPORT_AGENT, true);
        when(userRepository.findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true)).thenReturn(List.of(u));

        List<UserDTO> dtos = adminUserService.getAllAgents();

        assertEquals(1, dtos.size());
        verify(userRepository).findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true);
    }

    @Test
    void getAllManagers_mapsToUserDTO() {
        User u = user(UUID.randomUUID(), UserRole.SUPPORT_MANAGER, true);
        when(userRepository.findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true)).thenReturn(List.of(u));

        List<UserDTO> dtos = adminUserService.getAllManagers();

        assertEquals(1, dtos.size());
        verify(userRepository).findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true);
    }
}
