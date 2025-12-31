package com.ticket.service;

import com.ticket.dto.UserDTO;
import com.ticket.entity.User;
import com.ticket.enums.UserRole;
import com.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all agents (active only)
     */
    public List<UserDTO> getAllAgents() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_AGENT, true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all managers (active only)
     */
    public List<UserDTO> getAllManagers() {
        return userRepository.findByRoleAndIsActive(UserRole.SUPPORT_MANAGER, true)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
}
