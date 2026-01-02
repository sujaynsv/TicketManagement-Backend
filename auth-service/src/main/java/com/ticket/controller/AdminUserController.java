package com.ticket.controller;

import com.ticket.dto.*;
import com.ticket.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    
    @Autowired
    private AdminUserService adminUserService;
    
    /**
     * Get all users with pagination and filtering
     * GET /admin/users?page=0&size=10&role=SUPPORT_AGENT&isActive=true&search=john
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search
    ) {
        Page<AdminUserDTO> users = adminUserService.getAllUsers(page, size, role, isActive, search);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID
     * GET /admin/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable String userId) {
        AdminUserDTO user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Create new user
     * POST /admin/users
     */
    @PostMapping
    public ResponseEntity<AdminUserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        AdminUserDTO user = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    /**
     * Update user details
     * PUT /admin/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<AdminUserDTO> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        AdminUserDTO user = adminUserService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Change user role
     * PUT /admin/users/{userId}/role
     */
    @PutMapping("/{userId}/role")
    public ResponseEntity<AdminUserDTO> changeUserRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        AdminUserDTO user = adminUserService.changeUserRole(userId, request.role());
        return ResponseEntity.ok(user);
    }
    
    /**
     * Activate user
     * PUT /admin/users/{userId}/activate
     */
    @PutMapping("/{userId}/activate")
    public ResponseEntity<AdminUserDTO> activateUser(@PathVariable String userId) {
        AdminUserDTO user = adminUserService.activateUser(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Deactivate user
     * PUT /admin/users/{userId}/deactivate
     */
    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<AdminUserDTO> deactivateUser(@PathVariable String userId) {
        AdminUserDTO user = adminUserService.deactivateUser(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Assign manager to user
     * PUT /admin/users/{userId}/manager
     */
    @PutMapping("/{userId}/manager")
    public ResponseEntity<AdminUserDTO> assignManager(
            @PathVariable String userId,
            @Valid @RequestBody AssignManagerRequest request
    ) {
        AdminUserDTO user = adminUserService.assignManager(userId, request.managerId());
        return ResponseEntity.ok(user);
    }
    
    /**
     * Reset user password
     * PUT /admin/users/{userId}/reset-password
     */
    @PutMapping("/{userId}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        String message = adminUserService.resetPassword(userId, request.newPassword());
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Get user statistics
     * GET /admin/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatsDTO> getUserStats() {
        UserStatsDTO stats = adminUserService.getUserStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get all agents (for dropdowns)
     * GET /admin/users/agents
     */
    @GetMapping("/agents")
    public ResponseEntity<List<UserDTO>> getAllAgents() {
        List<UserDTO> agents = adminUserService.getAllAgents();
        return ResponseEntity.ok(agents);
    }
    
    /**
     * Get all managers (for dropdowns)
     * GET /admin/users/managers
     */
    @GetMapping("/managers")
    public ResponseEntity<List<UserDTO>> getAllManagers() {
        List<UserDTO> managers = adminUserService.getAllManagers();
        return ResponseEntity.ok(managers);
    }
}
