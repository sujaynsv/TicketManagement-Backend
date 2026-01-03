package com.notification.service;

import com.notification.dto.NotificationDTO;
import com.notification.dto.UserDTO;
import com.notification.entity.*;
import com.notification.repository.NotificationRepository;
import com.ticket.event.CommentAddedEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationService {
    
    private NotificationRepository notificationRepository;
    
    private EmailService emailService;
    
    private UserService userService;

    public NotificationService(UserService usersService, EmailService emailService, NotificationRepository notificationRepository){
        this.userService=usersService;
        this.emailService=emailService;
        this.notificationRepository=notificationRepository;
    }
    
    @Value("${notification.retry.max-attempts:3}")
    private Integer maxRetryAttempts;
    
    /**
     * Create and send notification
     */
    @Transactional
    public Notification createNotification(String userId, String username,
                                          NotificationType notificationType,
                                          String eventType,
                                          String ticketId, String ticketNumber,
                                          String subject, String message,
                                          DeliveryChannel deliveryChannel) {
        
        log.info("Creating notification for user {}: {}", username, notificationType);
        
        // Get user details
        UserDTO user = userService.getUserWithFallback(userId, username);
        
        // Create notification entity
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserEmail(user.getEmail());
        notification.setUsername(username);
        notification.setNotificationType(notificationType);
        notification.setEventType(eventType);
        notification.setTicketId(ticketId);
        notification.setTicketNumber(ticketNumber);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setDeliveryChannel(deliveryChannel);
        notification.setStatus(NotificationStatus.PENDING);
        
        // Save notification
        Notification savedNotification = notificationRepository.save(notification);
        
        // Send notification asynchronously
        sendNotification(savedNotification);
        
        return savedNotification;
    }
    
    /**
     * Send notification based on delivery channel
     */
    private void sendNotification(Notification notification) {
        try {
            if (notification.getDeliveryChannel() == DeliveryChannel.EMAIL) {
                sendEmailNotification(notification);
            } else if (notification.getDeliveryChannel() == DeliveryChannel.IN_APP) {
                // In-app notification is already stored in database
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", 
                     notification.getNotificationId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Send email notification (plain text)
     */
    private void sendEmailNotification(Notification notification) {
        try {
            emailService.sendEmail(
                notification.getUserEmail(),
                notification.getSubject(),
                notification.getMessage()
            );
            
            // Update notification status
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            log.info("Email notification sent to {}", notification.getUserEmail());
            
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Get user notifications
     */
    public List<NotificationDTO> getUserNotifications(String userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get unread notifications
     */
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }
    
    /**
     * Mark all user notifications as read
     */
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        notifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
        
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void handleCommentAdded(CommentAddedEvent event){
        log.info("Processing Comment added : {}", event.getTicketNumber());
    }
    
    /**
     * Get unread count
     */
    public Long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * Convert to DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationId(notification.getNotificationId());
        dto.setUserId(notification.getUserId());
        dto.setUsername(notification.getUsername());
        dto.setNotificationType(notification.getNotificationType().name());
        dto.setEventType(notification.getEventType());
        dto.setTicketId(notification.getTicketId());
        dto.setTicketNumber(notification.getTicketNumber());
        dto.setSubject(notification.getSubject());
        dto.setMessage(notification.getMessage());
        dto.setDeliveryChannel(notification.getDeliveryChannel().name());
        dto.setStatus(notification.getStatus().name());
        dto.setIsRead(notification.getIsRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setSentAt(notification.getSentAt());
        return dto;
    }
}
