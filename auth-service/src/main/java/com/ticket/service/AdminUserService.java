package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserService {
    
    @Autowired
    private UserRepository userRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    /**
     * Get all users with pagination and filtering
     */
    public Page<AdminUserDTO> getAllUsers(int page, int size, String role, Boolean isActive, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Specification<User> spec = Specification.where(null);
        
        // Filter by role
        if (role != null && !role.isBlank()) {
            UserRole userRole = UserRole.fromString(role);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), userRole));
        }
        
        // Filter by active status
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        
        // Search by username, email, firstName, lastName
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("firstName")), searchPattern),
                    cb.like(cb.lower(root.get("lastName")), searchPattern)
            ));
        }
        
        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(this::convertToAdminDTO);
    }
    
    /**
     * Get user by ID
     */
    public AdminUserDTO getUserById(String userId) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToAdminDTO(user);
    }
    
    /**
     * Create new user
     */
    @Transactional
    public AdminUserDTO createUser(CreateUserRequest request) {
        // Validate username
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Validate email
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }
        
        UserRole role = UserRole.fromString(request.role());
        
        // Validate manager assignment rules
        if (request.managerId() != null && !request.managerId().isBlank()) {
            // ADMIN and SUPPORT_MANAGER cannot have managers
            if (role == UserRole.ADMIN || role == UserRole.SUPPORT_MANAGER) {
                throw new RuntimeException("Admins and Managers cannot be assigned a manager");
            }
        }
        
        // SUPPORT_AGENT must have a manager
        if (role == UserRole.SUPPORT_AGENT && (request.managerId() == null || request.managerId().isBlank())) {
            throw new RuntimeException("Support Agents must be assigned a manager");
        }
        
        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setFirstName(request.firstName());
        newUser.setLastName(request.lastName());
        newUser.setRole(role);
        newUser.setIsActive(true);
        
        // Set manager if provided and allowed
        if (request.managerId() != null && !request.managerId().isBlank()) {
            User manager = userRepository.findById(UUID.fromString(request.managerId()))
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
            
            // Validate manager role
            if (manager.getRole() != UserRole.SUPPORT_MANAGER && manager.getRole() != UserRole.ADMIN) {
                throw new RuntimeException("Assigned manager must have SUPPORT_MANAGER or ADMIN role");
            }
            
            // Validate manager is active
            if (!manager.getIsActive()) {
                throw new RuntimeException("Cannot assign an inactive manager");
            }
            
            newUser.setManager(manager);
        }
        
        userRepository.save(newUser);
        return convertToAdminDTO(newUser);
    }

    
    /**
     * Update user details
     */
    @Transactional
    public AdminUserDTO updateUser(String userId, UpdateUserRequest request) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update email if changed
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.email());
        }
        
        // Update other fields
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        
        userRepository.save(user);
        return convertToAdminDTO(user);
    }
    
    /**
     * Change user role
     */
    @Transactional
    public AdminUserDTO changeUserRole(String userId, String newRole) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserRole role = UserRole.fromString(newRole);
        user.setRole(role);
        
        userRepository.save(user);
        return convertToAdminDTO(user);
    }
    
    /**
     * Activate user
     */
    @Transactional
    public AdminUserDTO activateUser(String userId) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(true);
        userRepository.save(user);
        return convertToAdminDTO(user);
    }
    
    /**
     * Deactivate user
     */
    @Transactional
    public AdminUserDTO deactivateUser(String userId) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        return convertToAdminDTO(user);
    }
    
    /**
     * Assign manager to user
     */
    @Transactional
    public AdminUserDTO assignManager(String userId, String managerId) {
        UUID userUuid = UUID.fromString(userId);
        UUID managerUuid = UUID.fromString(managerId);
        
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User manager = userRepository.findById(managerUuid)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        
        // Validate manager role
        if (manager.getRole() != UserRole.SUPPORT_MANAGER && manager.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Selected user is not a manager");
        }
        
        user.setManager(manager);
        userRepository.save(user);
        return convertToAdminDTO(user);
    }
    
    /**
     * Reset user password
     */
    @Transactional
    public String resetPassword(String userId, String newPassword) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        return "Password reset successfully";
    }
    
    /**
     * Get user statistics
     */
    public UserStatsDTO getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long inactiveUsers = userRepository.countByIsActive(false);
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        long managerCount = userRepository.countByRole(UserRole.SUPPORT_MANAGER);
        long agentCount = userRepository.countByRole(UserRole.SUPPORT_AGENT);
        long endUserCount = userRepository.countByRole(UserRole.END_USER);
        
        return new UserStatsDTO(
                totalUsers,
                activeUsers,
                inactiveUsers,
                adminCount,
                managerCount,
                agentCount,
                endUserCount
        );
    }
    
    /**
     * Get all agents
     */
    public List<UserDTO> getAllAgents() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all managers
     */
    public List<UserDTO> getAllManagers() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert User to AdminUserDTO
     */
    private AdminUserDTO convertToAdminDTO(User user) {
        String managerId = null;
        String managerName = null;
        
        if (user.getManager() != null) {
            managerId = user.getManager().getUserId().toString();
            managerName = user.getManager().getFirstName() + " " + user.getManager().getLastName();
        }
        
        return new AdminUserDTO(
                user.getUserId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getIsActive(),
                managerId,
                managerName,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLogin()
        );
    }
    
    /**
     * Convert User to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getUserId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }
}
