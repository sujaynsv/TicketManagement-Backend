package com.notification.repository;

import com.notification.entity.Notification;
import com.notification.entity.NotificationStatus;
import com.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    /**
     * Find notifications by user ID
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find unread notifications by user ID
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
    
    /**
     * Find notifications by ticket ID
     */
    List<Notification> findByTicketIdOrderByCreatedAtDesc(String ticketId);
    
    /**
     * Find notifications by status
     */
    List<Notification> findByStatus(NotificationStatus status);
    
    /**
     * Find pending or failed notifications for retry
     */
    @Query("SELECT n FROM Notification n WHERE n.status IN ('PENDING', 'FAILED', 'RETRY') " +
           "AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<Notification> findNotificationsForRetry(Integer maxRetries);
    
    /**
     * Find notifications by user and notification type
     */
    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            String userId, NotificationType notificationType);
    
    /**
     * Count unread notifications for a user
     */
    Long countByUserIdAndIsReadFalse(String userId);
    
    /**
     * Find notifications created between dates
     */
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
