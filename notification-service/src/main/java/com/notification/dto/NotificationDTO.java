package com.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    private String notificationId;
    private String userId;
    private String username;
    private String notificationType;
    private String eventType;
    private String ticketId;
    private String ticketNumber;
    private String subject;
    private String message;
    private String deliveryChannel;
    private String status;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
