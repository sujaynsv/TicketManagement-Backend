package com.ticket.service;

import com.ticket.dto.UpdateProfileRequest;
import com.ticket.dto.UserDTO;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    
    private UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository=userRepository;
    }
    
    /**
     * Get all agents (active only)
     */
    public List<UserDTO> getAllAgents() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get all managers (active only)
     */
    public List<UserDTO> getAllManagers() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get user by ID
     */
    public UserDTO getUserById(String userId) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }
    
    /**
     * Convert User entity to DTO
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

    public UserDTO updateUserProfile(String userId, UpdateProfileRequest request) {
        UUID uuid = UUID.fromString(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Update only provided fields
    if (request.getName() != null && !request.getName().isBlank()) {
        user.setUsername(request.getName());
    }
    
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
        // Check if email already exists (for another user)
        userRepository.findByEmail(request.getEmail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getUserId().equals(userId)) {
                        throw new RuntimeException("Email already in use");
                    }
                });
        user.setEmail(request.getEmail());
    }
    
    
    user.setUpdatedAt(LocalDateTime.now());
    User savedUser = userRepository.save(user);
    
    return convertToDTO(savedUser);
}

}
