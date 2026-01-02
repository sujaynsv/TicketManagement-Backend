package com.notification.listener;

import com.notification.client.TicketServiceClient;
import com.notification.dto.TicketDTO;
import com.notification.entity.DeliveryChannel;
import com.notification.entity.NotificationType;
import com.notification.service.NotificationService;
import com.ticket.event.CommentAddedEvent;
import com.ticket.event.SlaBreachEvent;
import com.ticket.event.SlaWarningEvent;
import com.ticket.event.TicketAssignedEvent;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RabbitListener(queues = "notification.queue")
@RequiredArgsConstructor
public class EventListener {
    
    private final NotificationService notificationService;
    
    /**
     * Handle TicketCreatedEvent
     * Notify: User who created the ticket
     */

    @Autowired
    private TicketServiceClient ticketServiceClient;

    @RabbitHandler
    public void handleTicketCreated(TicketCreatedEvent event) {
        try {
            log.info("Received TicketCreatedEvent: {}", event.getTicketNumber());
            
            // Notify ticket creator
            String subject = "Ticket Created: " + event.getTicketNumber();
            String message = String.format(
                "Hello %s,\n\n" +
                "Your ticket has been created successfully.\n\n" +
                "Ticket Details:\n" +
                "----------------\n" +
                "Ticket Number: %s\n" +
                "Title: %s\n" +
                "Category: %s\n" +
                "Priority: %s\n\n" +
                "Our support team will review and assign it shortly.\n\n" +
                "You will receive notifications when:\n" +
                "- Your ticket is assigned to an agent\n" +
                "- The agent responds to your ticket\n" +
                "- The status of your ticket changes\n\n" +
                "Thank you,\n" +
                "Ticket Management System",
                event.getCreatedByUsername(),
                event.getTicketNumber(),
                event.getTitle(),
                event.getCategory(),
                event.getPriority() != null ? event.getPriority() : "Not set"
            );
            
            // Send email notification
            notificationService.createNotification(
                event.getCreatedByUserId(),
                event.getCreatedByUsername(),
                NotificationType.TICKET_CREATED,
                "TICKET_CREATED",
                event.getTicketId(),
                event.getTicketNumber(),
                subject,
                message,
                DeliveryChannel.EMAIL
            );
            
            // Send in-app notification
            String inAppMessage = String.format(
                "Your ticket %s has been created. Category: %s, Priority: %s",
                event.getTicketNumber(),
                event.getCategory(),
                event.getPriority() != null ? event.getPriority() : "Not set"
            );
            
            notificationService.createNotification(
                event.getCreatedByUserId(),
                event.getCreatedByUsername(),
                NotificationType.TICKET_CREATED,
                "TICKET_CREATED",
                event.getTicketId(),
                event.getTicketNumber(),
                "Ticket Created",
                inAppMessage,
                DeliveryChannel.IN_APP
            );
            
            log.info("Notifications sent for ticket creation: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling TicketCreatedEvent for {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle TicketAssignedEvent
     * Notify: Agent who received the ticket
     */
    @RabbitHandler
    public void handleTicketAssigned(TicketAssignedEvent event) {
        try {
            log.info("Received TicketAssignedEvent: {} assigned to {}", 
                     event.getTicketNumber(), event.getAssignedToUsername());
            
            // Validate event has assigned agent info
            if (event.getAssignedToUserId() == null) {
                log.warn("Skipping TicketAssignedEvent - missing assigned agent info");
                return;
            }
            
            // Notify Agent
            String agentSubject = "New Ticket Assigned: " + event.getTicketNumber();
            String agentMessage = String.format(
                "Hello %s,\n\n" +
                "A new ticket has been assigned to you.\n\n" +
                "Ticket Details:\n" +
                "----------------\n" +
                "Ticket Number: %s\n" +
                "Assignment Type: %s\n" +
                "Assigned By: %s\n" +
                "Assigned At: %s\n\n" +
                "Action Required:\n" +
                "- Review the ticket details\n" +
                "- Respond to the customer within the SLA timeframe\n" +
                "- Update the ticket status as you progress\n\n" +
                "Please log in to the system to view the full details.\n\n" +
                "Thank you,\n" +
                "Ticket Management System",
                event.getAssignedToUsername(),
                event.getTicketNumber(),
                event.getAssignmentType(),
                event.getAssignedByUsername(),
                event.getAssignedAt()
            );
            
            // Email to agent
            notificationService.createNotification(
                event.getAssignedToUserId(),
                event.getAssignedToUsername(),
                NotificationType.TICKET_ASSIGNED,
                "TICKET_ASSIGNED",
                event.getTicketId(),
                event.getTicketNumber(),
                agentSubject,
                agentMessage,
                DeliveryChannel.EMAIL
            );
            
            // In-app notification for agent
            String agentInAppMessage = String.format(
                "New ticket %s assigned to you by %s",
                event.getTicketNumber(),
                event.getAssignedByUsername()
            );
            
            notificationService.createNotification(
                event.getAssignedToUserId(),
                event.getAssignedToUsername(),
                NotificationType.TICKET_ASSIGNED,
                "TICKET_ASSIGNED",
                event.getTicketId(),
                event.getTicketNumber(),
                "New Ticket Assigned",
                agentInAppMessage,
                DeliveryChannel.IN_APP
            );
            
            log.info("Agent notification sent for ticket assignment: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling TicketAssignedEvent for {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle TicketStatusChangedEvent
     * Notify: User who changed the status (in-app only)
     */
    @RabbitHandler
    public void handleTicketStatusChanged(TicketStatusChangedEvent event) {
        try {
            log.info("Received TicketStatusChangedEvent: {} changed from {} to {}", 
                     event.getTicketNumber(), event.getOldStatus(), event.getNewStatus());
            
            String subject = "Ticket Status Updated: " + event.getTicketNumber();
            String message = String.format(
                "Hello %s,\n\n" +
                "The status of ticket %s has been updated.\n\n" +
                "Status Change:\n" +
                "----------------\n" +
                "Old Status: %s\n" +
                "New Status: %s\n" +
                "Changed By: %s\n" +
                "Changed At: %s\n\n" +
                "Comment: %s\n\n" +
                "Thank you,\n" +
                "Ticket Management System",
                event.getChangedByUsername(),
                event.getTicketNumber(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getChangedByUsername(),
                event.getChangedAt(),
                event.getComment() != null ? event.getComment() : "No comment provided"
            );
            
            NotificationType notificationType = getNotificationTypeForStatus(event.getNewStatus());
            
            // In-app notification
            String inAppMessage = String.format(
                "Ticket %s status changed: %s to %s",
                event.getTicketNumber(),
                event.getOldStatus(),
                event.getNewStatus()
            );
            
            notificationService.createNotification(
                event.getChangedByUserId(),
                event.getChangedByUsername(),
                notificationType,
                "TICKET_STATUS_CHANGED",
                event.getTicketId(),
                event.getTicketNumber(),
                "Status Updated",
                inAppMessage,
                DeliveryChannel.IN_APP
            );
            
            // If ticket is resolved, send email
            if ("RESOLVED".equals(event.getNewStatus())) {
                notificationService.createNotification(
                    event.getChangedByUserId(),
                    event.getChangedByUsername(),
                    NotificationType.TICKET_RESOLVED,
                    "TICKET_RESOLVED",
                    event.getTicketId(),
                    event.getTicketNumber(),
                    subject,
                    message,
                    DeliveryChannel.EMAIL
                );
            }
            
            log.info("Status change notification sent for ticket: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling TicketStatusChangedEvent for {}: {}", 
                     event.getTicketNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle CommentAddedEvent
     * Notify: Ticket creator + Assigned agent (excluding comment author)
     */
    // @RabbitHandler
    // public void handleCommentAdded(CommentAddedEvent event) {
    //     try {
    //         log.info("Received CommentAddedEvent: {} by {}", 
    //                  event.getTicketNumber(), event.getUsername());
            
    //         // Only notify for non-internal comments
    //         if (event.getIsInternal() != null && event.getIsInternal()) {
    //             log.info("Internal comment, skipping notification");
    //             return;
    //         }
            
    //         String subject = "New Comment on Ticket: " + event.getTicketNumber();
    //         String message = String.format(
    //             "Hello,\n\n" +
    //             "A new comment has been added to ticket %s.\n\n" +
    //             "Comment Details:\n" +
    //             "----------------\n" +
    //             "Comment By: %s\n" +
    //             "Added At: %s\n\n" +
    //             "Comment:\n" +
    //             "%s\n\n" +
    //             "Please log in to view the full conversation.\n\n" +
    //             "Thank you,\n" +
    //             "Ticket Management System",
    //             event.getTicketNumber(),
    //             event.getUsername(),
    //             event.getCreatedAt(),
    //             event.getCommentText()
    //         );
            
    //         // Create in-app notification for comment author
    //         String inAppMessage = String.format(
    //             "You added a comment on ticket %s",
    //             event.getTicketNumber()
    //         );
            
    //         notificationService.createNotification(
    //             event.getUserId(),
    //             event.getUsername(),
    //             NotificationType.COMMENT_ADDED,
    //             "COMMENT_ADDED",
    //             event.getTicketId(),
    //             event.getTicketNumber(),
    //             "Comment Added",
    //             inAppMessage,
    //             DeliveryChannel.IN_APP
    //         );
            
    //         log.info("Comment notification sent for ticket: {}", event.getTicketNumber());
            
    //     } catch (Exception e) {
    //         log.error("Error handling CommentAddedEvent for {}: {}", 
    //                  event.getTicketNumber(), e.getMessage(), e);
    //     }
    // }
    
    /**
     * Get notification type based on ticket status
     */
    private NotificationType getNotificationTypeForStatus(String status) {
        return switch (status) {
            case "RESOLVED" -> NotificationType.TICKET_RESOLVED;
            case "CLOSED" -> NotificationType.TICKET_CLOSED;
            default -> NotificationType.TICKET_STATUS_CHANGED;
        };
    }

    @RabbitHandler
public void handleSlaWarning(SlaWarningEvent event) {
    try {
        log.info("Received SlaWarningEvent: {} - {} warning", 
                 event.getTicketNumber(), event.getWarningType());
        
        if (event.getAssignedAgentId() == null) {
            log.warn("Skipping SlaWarningEvent - no agent assigned yet");
            return;
        }
        
        String subject = String.format("SLA Warning: Ticket %s (%s)", 
                                      event.getTicketNumber(), event.getWarningType());
        
        String message = String.format(
            "Hello %s,\n\n" +
            "SLA WARNING for ticket %s\n\n" +
            "Warning Details:\n" +
            "----------------\n" +
            "Ticket Number: %s\n" +
            "Priority: %s\n" +
            "Category: %s\n" +
            "SLA Type: %s\n" +
            "Due At: %s\n" +
            "Time Remaining: %d minutes\n" +
            "Time Used: %.1f%%\n\n" +
            "URGENT ACTION REQUIRED:\n" +
            "This ticket is approaching its SLA deadline. Please take immediate action to avoid SLA breach.\n\n" +
            "Thank you,\n" +
            "Ticket Management System",
            event.getAssignedAgentUsername(),
            event.getTicketNumber(),
            event.getTicketNumber(),
            event.getPriority(),
            event.getCategory(),
            event.getWarningType(),
            event.getDueAt(),
            event.getMinutesRemaining(),
            event.getPercentageTimeUsed()
        );
        
        notificationService.createNotification(
            event.getAssignedAgentId(),
            event.getAssignedAgentUsername(),
            NotificationType.SLA_WARNING,
            "SLA_WARNING",
            event.getTicketId(),
            event.getTicketNumber(),
            subject,
            message,
            DeliveryChannel.EMAIL
        );
        
        String inAppMessage = String.format(
            "SLA Warning: Ticket %s - %s SLA at %.0f%% (Due: %s)",
            event.getTicketNumber(),
            event.getWarningType(),
            event.getPercentageTimeUsed(),
            event.getDueAt()
        );
        
        notificationService.createNotification(
            event.getAssignedAgentId(),
            event.getAssignedAgentUsername(),
            NotificationType.SLA_WARNING,
            "SLA_WARNING",
            event.getTicketId(),
            event.getTicketNumber(),
            "SLA Warning",
            inAppMessage,
            DeliveryChannel.IN_APP
        );
        
        log.info("SLA warning notification sent for ticket: {}", event.getTicketNumber());
        
    } catch (Exception e) {
        log.error("Error handling SlaWarningEvent for {}: {}", 
                 event.getTicketNumber(), e.getMessage(), e);
    }
}

    @RabbitHandler
    public void handleSlaBreach(SlaBreachEvent event) {
        try {
            log.info("Received SlaBreachEvent: {} - {} breach", 
                    event.getTicketNumber(), event.getBreachType());
            
            if (event.getAssignedAgentId() == null) {
                log.warn("Skipping SlaBreachEvent - no agent assigned");
                return;
            }
            
            String subject = String.format("SLA BREACHED: Ticket %s (%s)", 
                                        event.getTicketNumber(), event.getBreachType());
            
            String message = String.format(
                "Hello %s,\n\n" +
                "CRITICAL: SLA BREACH for ticket %s\n\n" +
                "Breach Details:\n" +
                "----------------\n" +
                "Ticket Number: %s\n" +
                "Priority: %s\n" +
                "Category: %s\n" +
                "Breach Type: %s\n" +
                "Was Due At: %s\n" +
                "Breached At: %s\n" +
                "Minutes Overdue: %d\n" +
                "Breach Reason: %s\n\n" +
                "IMMEDIATE ESCALATION REQUIRED:\n" +
                "This ticket has breached its SLA commitment. Please escalate to management immediately and take corrective action.\n\n" +
                "Thank you,\n" +
                "Ticket Management System",
                event.getAssignedAgentUsername(),
                event.getTicketNumber(),
                event.getTicketNumber(),
                event.getPriority(),
                event.getCategory(),
                event.getBreachType(),
                event.getDueAt(),
                event.getBreachedAt(),
                event.getMinutesOverdue(),
                event.getBreachReason()
            );
            
            notificationService.createNotification(
                event.getAssignedAgentId(),
                event.getAssignedAgentUsername(),
                NotificationType.SLA_BREACH,
                "SLA_BREACH",
                event.getTicketId(),
                event.getTicketNumber(),
                subject,
                message,
                DeliveryChannel.EMAIL
            );
            
            String inAppMessage = String.format(
                "SLA BREACH: Ticket %s - %s SLA overdue by %d minutes",
                event.getTicketNumber(),
                event.getBreachType(),
                event.getMinutesOverdue()
            );
            
            notificationService.createNotification(
                event.getAssignedAgentId(),
                event.getAssignedAgentUsername(),
                NotificationType.SLA_BREACH,
                "SLA_BREACH",
                event.getTicketId(),
                event.getTicketNumber(),
                "SLA BREACH",
                inAppMessage,
                DeliveryChannel.IN_APP
            );
            
            log.info("SLA breach notification sent for ticket: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling SlaBreachEvent for {}: {}", 
                    event.getTicketNumber(), e.getMessage(), e);
        }
    }


    @RabbitHandler
    public void handleCommentAdded(CommentAddedEvent event) {
        try {
            log.info("Received CommentAddedEvent: {} by {}", 
                    event.getTicketNumber(), event.getUsername());
            
            if (event.getIsInternal() != null && event.getIsInternal()) {
                log.info("Internal comment, skipping external notifications");
                return;
            }
            
            TicketDTO ticket;
            try {
                ticket = ticketServiceClient.getTicket(event.getTicketId());
            } catch (Exception e) {
                log.error("Failed to fetch ticket {} from ticket-service: {}", 
                        event.getTicketId(), e.getMessage());
                return;
            }
            
            if (ticket == null) {
                log.warn("Ticket {} not found", event.getTicketNumber());
                return;
            }
            
            String subject = "New Comment on Ticket: " + event.getTicketNumber();
            
            if (ticket.createdByUserId() != null && 
                !ticket.createdByUserId().equals(event.getUserId())) {
                
                String creatorMessage = String.format(
                    "Hello %s,\n\n" +
                    "A new comment has been added to your ticket %s.\n\n" +
                    "Comment Details:\n" +
                    "----------------\n" +
                    "Comment By: %s\n" +
                    "Added At: %s\n\n" +
                    "Comment:\n" +
                    "%s\n\n" +
                    "Please log in to view the full conversation and respond if needed.\n\n" +
                    "Thank you,\n" +
                    "Ticket Management System",
                    ticket.createdByUsername(),
                    event.getTicketNumber(),
                    event.getUsername(),
                    event.getCreatedAt(),
                    event.getCommentText()
                );
                
                notificationService.createNotification(
                    ticket.createdByUserId(),
                    ticket.createdByUsername(),
                    NotificationType.COMMENT_ADDED,
                    "COMMENT_ADDED",
                    event.getTicketId(),
                    event.getTicketNumber(),
                    subject,
                    creatorMessage,
                    DeliveryChannel.EMAIL
                );
                
                String creatorInAppMessage = String.format(
                    "%s commented on your ticket %s",
                    event.getUsername(),
                    event.getTicketNumber()
                );
                
                notificationService.createNotification(
                    ticket.createdByUserId(),
                    ticket.createdByUsername(),
                    NotificationType.COMMENT_ADDED,
                    "COMMENT_ADDED",
                    event.getTicketId(),
                    event.getTicketNumber(),
                    "New Comment",
                    creatorInAppMessage,
                    DeliveryChannel.IN_APP
                );
                
                log.info("Comment notification sent to ticket creator: {}", 
                        ticket.createdByUsername());
            }
            
            if (ticket.assignedToUserId() != null && 
                !ticket.assignedToUserId().equals(event.getUserId()) &&
                !ticket.assignedToUserId().equals(ticket.createdByUserId())) {
                
                String agentMessage = String.format(
                    "Hello %s,\n\n" +
                    "A new comment has been added to ticket %s assigned to you.\n\n" +
                    "Comment Details:\n" +
                    "----------------\n" +
                    "Comment By: %s\n" +
                    "Added At: %s\n\n" +
                    "Comment:\n" +
                    "%s\n\n" +
                    "Please review and respond as needed.\n\n" +
                    "Thank you,\n" +
                    "Ticket Management System",
                    ticket.assignedToUsername(),
                    event.getTicketNumber(),
                    event.getUsername(),
                    event.getCreatedAt(),
                    event.getCommentText()
                );
                
                notificationService.createNotification(
                    ticket.assignedToUserId(),
                    ticket.assignedToUsername(),
                    NotificationType.COMMENT_ADDED,
                    "COMMENT_ADDED",
                    event.getTicketId(),
                    event.getTicketNumber(),
                    subject,
                    agentMessage,
                    DeliveryChannel.EMAIL
                );
                
                String agentInAppMessage = String.format(
                    "%s commented on ticket %s",
                    event.getUsername(),
                    event.getTicketNumber()
                );
                
                notificationService.createNotification(
                    ticket.assignedToUserId(),
                    ticket.assignedToUsername(),
                    NotificationType.COMMENT_ADDED,
                    "COMMENT_ADDED",
                    event.getTicketId(),
                    event.getTicketNumber(),
                    "New Comment",
                    agentInAppMessage,
                    DeliveryChannel.IN_APP
                );
                
                log.info("Comment notification sent to assigned agent: {}", 
                        ticket.assignedToUsername());
            }
            
            log.info("Comment notifications processed for ticket: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling CommentAddedEvent for {}: {}", 
                    event.getTicketNumber(), e.getMessage(), e);
        }
    }



}
