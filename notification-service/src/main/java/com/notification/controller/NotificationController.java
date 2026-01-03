package com.notification.controller;

import com.notification.dto.NotificationDTO;
import com.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {
    
    private NotificationService notificationService;

    public NotificationController(NotificationService notificationService){
        this.notificationService=notificationService;
    }
    
    /**
     * Get user notifications
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @PathVariable String userId) {
        
        log.info("Fetching notifications for user: {}", userId);
        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications
     */
    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @PathVariable String userId) {
        
        log.info("Fetching unread notifications for user: {}", userId);
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread count
     */
    @GetMapping("/users/{userId}/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable String userId) {
        
        Long count = notificationService.getUnreadCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("unreadCount", count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark notification as read
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable String notificationId) {
        
        log.info("Marking notification as read: {}", notificationId);
        notificationService.markAsRead(notificationId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        response.put("notificationId", notificationId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark all notifications as read
     */
    @PatchMapping("/users/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @PathVariable String userId) {
        
        log.info("Marking all notifications as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }
}
