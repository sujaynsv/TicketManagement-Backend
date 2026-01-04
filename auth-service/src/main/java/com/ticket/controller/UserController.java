package com.ticket.controller;

import com.ticket.dto.UserDTO;
import com.ticket.dto.UpdateProfileRequest;
import com.ticket.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    
    private UserService userService;

    public UserController(UserService userService){
        this.userService=userService;
    }
    
    /**
     * Get current user's profile (NEW!)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUserProfile(
            @RequestHeader("X-User-Id") String userId) {
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update current user's profile (NEW!)
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateProfileRequest request) {
        UserDTO user = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Get all agents (for Assignment Service)
     */
    @GetMapping("/agents")
    public ResponseEntity<List<UserDTO>> getAllAgents() {
        List<UserDTO> agents = userService.getAllAgents();
        return ResponseEntity.ok(agents);
    }
    
    /**
     * Get all managers (for future use)
     */
    @GetMapping("/managers")
    public ResponseEntity<List<UserDTO>> getAllManagers() {
        List<UserDTO> managers = userService.getAllManagers();
        return ResponseEntity.ok(managers);
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String userId) {
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
