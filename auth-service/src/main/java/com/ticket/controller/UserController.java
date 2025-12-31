package com.ticket.controller;

import com.ticket.dto.UserDTO;
import com.ticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
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
