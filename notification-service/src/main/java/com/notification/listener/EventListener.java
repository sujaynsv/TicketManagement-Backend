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
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RabbitListener(queues = "notification.queue")
@RequiredArgsConstructor
public class EventListener {
    
    private static final String COMMENT_ADDED_ACTION = "COMMENT_ADDED";
    private static final String TICKET_ESCALATED_ACTION = "TICKET_ESCALATED";

    private final NotificationService notificationService;
    
    /**
     * Handle TicketCreatedEvent
     * Notify: User who created the ticket
     */

    private final TicketServiceClient ticketServiceClient;

    @RabbitHandler
    public void handleTicketCreated(TicketCreatedEvent event) {
        try {
            log.info("Received TicketCreatedEvent: {}", event.getTicketNumber());
            
            // Notify ticket creator
            String subject = "Ticket Created: " + event.getTicketNumber();
            String message = String.format("""
                Hello %s,

                Your ticket has been created successfully.

                Ticket Details:
                ----------------
                Ticket Number: %s
                Title: %s
                Category: %s
                Priority: %s

                Our support team will review and assign it shortly.

                You will receive notifications when:
                - Your ticket is assigned to an agent
                - The agent responds to your ticket
                - The status of your ticket changes

                Thank you,
                Ticket Management System
                """,
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
            String agentMessage = String.format("""
                Hello %s,

                A new ticket has been assigned to you.

                Ticket Details:
                ----------------
                Ticket Number: %s
                Assignment Type: %s
                Assigned By: %s
                Assigned At: %s

                Action Required:
                - Review the ticket details
                - Respond to the customer within the SLA timeframe
                - Update the ticket status as you progress

                Please log in to the system to view the full details.

                Thank you,
                Ticket Management System
                """,
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
                String message = String.format("""
                Hello %s,

                The status of ticket %s has been updated.

                Status Change:
                ----------------
                Old Status: %s
                New Status: %s
                Changed By: %s
                Changed At: %s

                Comment: %s

                Thank you,
                Ticket Management System
                """,
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
        
        String message = String.format("""
            Hello %s,

            SLA WARNING for ticket %s

            Warning Details:
            ----------------
            Ticket Number: %s
            Priority: %s
            Category: %s
            SLA Type: %s
            Due At: %s
            Time Remaining: %d minutes
            Time Used: %.1f%%

            URGENT ACTION REQUIRED:
            This ticket is approaching its SLA deadline. Please take immediate action to avoid SLA breach.

            Thank you,
            Ticket Management System
            """,
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
            
            String message = String.format("""
                Hello %s,

                CRITICAL: SLA BREACH for ticket %s

                Breach Details:
                ----------------
                Ticket Number: %s
                Priority: %s
                Category: %s
                Breach Type: %s
                Was Due At: %s
                Breached At: %s
                Minutes Overdue: %d
                Breach Reason: %s

                IMMEDIATE ESCALATION REQUIRED:
                This ticket has breached its SLA commitment. Please escalate to management immediately and take corrective action.

                Thank you,
                Ticket Management System
                """,
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
                
                String creatorMessage = String.format("""
                    Hello %s,

                    A new comment has been added to your ticket %s.

                    Comment Details:
                    ----------------
                    Comment By: %s
                    Added At: %s

                    Comment:
                    %s

                    Please log in to view the full conversation and respond if needed.

                    Thank you,
                    Ticket Management System
                    """,
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
                    COMMENT_ADDED_ACTION,
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
                    COMMENT_ADDED_ACTION,
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
                
                String agentMessage = String.format("""
                    Hello %s,

                    A new comment has been added to ticket %s assigned to you.

                    Comment Details:
                    ----------------
                    Comment By: %s
                    Added At: %s

                    Comment:
                    %s

                    Please review and respond as needed.

                    Thank you,
                    Ticket Management System
                    """,
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
                    COMMENT_ADDED_ACTION,
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
                    COMMENT_ADDED_ACTION,
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

    @RabbitHandler
    public void handleTicketEscalated(TicketEscalatedEvent event) {
        try {
            log.info("Received TicketEscalatedEvent: {} escalated to {}", 
                    event.getTicketNumber(), event.getEscalatedToUsername());
            
            String escalationType = "MANUAL".equals(event.getEscalationType()) ? "Manual Escalation" : "Auto-Escalation (SLA Breach)";
            
            String managerSubject = String.format("Ticket Escalated to You: %s", event.getTicketNumber());
            String managerMessage = String.format("""
                Hello %s,

                A ticket has been escalated to you.

                Escalation Details:
                ----------------
                Ticket Number: %s
                Title: %s
                Category: %s
                Priority: %s
                Escalation Type: %s
                Escalated By: %s
                Escalation Reason: %s
                Previous Agent: %s
                Escalated At: %s

                Action Required:
                This ticket requires your immediate attention. Please review and take appropriate action.
                You can now resolve or close this ticket as needed.

                Thank you,
                Ticket Management System
                """,
                event.getEscalatedToUsername(),
                event.getTicketNumber(),
                event.getTitle(),
                event.getCategory(),
                event.getPriority(),
                escalationType,
                event.getEscalatedByUsername(),
                event.getEscalationReason(),
                event.getPreviousAgentUsername(),
                event.getEscalatedAt()
            );
            
            notificationService.createNotification(
                event.getEscalatedToUserId(),
                event.getEscalatedToUsername(),
                NotificationType.TICKET_ESCALATED,
                TICKET_ESCALATED_ACTION,
                event.getTicketId(),
                event.getTicketNumber(),
                managerSubject,
                managerMessage,
                DeliveryChannel.EMAIL
            );
            
            String managerInAppMessage = String.format(
                "Ticket %s has been escalated to you by %s",
                event.getTicketNumber(),
                event.getEscalatedByUsername()
            );
            
            notificationService.createNotification(
                event.getEscalatedToUserId(),
                event.getEscalatedToUsername(),
                NotificationType.TICKET_ESCALATED,
                TICKET_ESCALATED_ACTION,
                event.getTicketId(),
                event.getTicketNumber(),
                "Ticket Escalated",
                managerInAppMessage,
                DeliveryChannel.IN_APP
            );
            
            if (event.getPreviousAgentId() != null) {
                String agentSubject = String.format("Ticket Escalated: %s", event.getTicketNumber());
                String agentMessage = String.format("""
                    Hello %s,

                    The ticket assigned to you has been escalated to management.

                    Escalation Details:
                    ----------------
                    Ticket Number: %s
                    Title: %s
                    Escalated To: %s
                    Escalation Type: %s
                    Reason: %s

                    This ticket is now being handled by %s.

                    Thank you,
                    Ticket Management System
                    """,
                    event.getPreviousAgentUsername(),
                    event.getTicketNumber(),
                    event.getTitle(),
                    event.getEscalatedToUsername(),
                    escalationType,
                    event.getEscalationReason(),
                    event.getEscalatedToUsername()
                );
                
                notificationService.createNotification(
                    event.getPreviousAgentId(),
                    event.getPreviousAgentUsername(),
                    NotificationType.TICKET_ESCALATED,
                    TICKET_ESCALATED_ACTION,
                    event.getTicketId(),
                    event.getTicketNumber(),
                    agentSubject,
                    agentMessage,
                    DeliveryChannel.EMAIL
                );
                
                String agentInAppMessage = String.format(
                    "Your ticket %s has been escalated to %s",
                    event.getTicketNumber(),
                    event.getEscalatedToUsername()
                );
                
                notificationService.createNotification(
                    event.getPreviousAgentId(),
                    event.getPreviousAgentUsername(),
                    NotificationType.TICKET_ESCALATED,
                    TICKET_ESCALATED_ACTION,
                    event.getTicketId(),
                    event.getTicketNumber(),
                    "Ticket Escalated",
                    agentInAppMessage,
                    DeliveryChannel.IN_APP
                );
            }
            
            log.info("Escalation notifications sent for ticket: {}", event.getTicketNumber());
            
        } catch (Exception e) {
            log.error("Error handling TicketEscalatedEvent for {}: {}", 
                    event.getTicketNumber(), e.getMessage(), e);
        }
    }



}
