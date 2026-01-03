package com.ticket.service;

import com.ticket.client.AssignmentServiceClient;
import com.ticket.client.UserServiceClient;
import com.ticket.dto.AssignmentDTO;
import com.ticket.dto.EscalateTicketRequest;
import com.ticket.dto.UserDTO;
import com.ticket.entity.Ticket;
import com.ticket.enums.EscalationType;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketEscalatedEvent;
import com.ticket.exception.TicketEscalationException;
import com.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {
    
    private final TicketRepository ticketRepository;
    private final AssignmentServiceClient assignmentServiceClient;
    private final UserServiceClient userServiceClient;
    private final EventPublisherService eventPublisherService;
    
    public Ticket escalateTicket(String ticketId, String escalatedBy, String escalatedByUsername, 
                                 EscalateTicketRequest request, EscalationType escalationType) {
        
        log.info("Escalating ticket: {} by {}", ticketId, escalatedByUsername);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        
        if (ticket.getStatus() == TicketStatus.ESCALATED) {
            throw new TicketEscalationException("Ticket is already escalated");
        }
        
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new TicketEscalationException("Cannot escalate a closed ticket");
        }
        
        AssignmentDTO assignment;
        try {
            assignment = assignmentServiceClient.getAssignmentByTicketId(ticketId);
        } catch (Exception e) {
            log.error("Failed to fetch assignment for ticket {}: {}", ticketId, e.getMessage());
            throw new TicketEscalationException("Ticket is not assigned. Cannot escalate unassigned ticket.");
        }
        
        if (assignment == null) {
            throw new TicketEscalationException("Ticket is not assigned. Cannot escalate unassigned ticket.");
        }
        
        String managerId = assignment.getAssignedBy();
        String managerUsername = assignment.getAssignedByUsername();
        
        UserDTO manager;
        try {
            manager = userServiceClient.getUserById(managerId);
        } catch (Exception e) {
            log.error("Failed to fetch manager details: {}", e.getMessage());
            throw new TicketEscalationException("Failed to fetch manager details");
        }
        
        String previousAgentId = ticket.getAssignedToUserId();
        String previousAgentUsername = ticket.getAssignedToUsername();
        
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setEscalatedToUserId(managerId);
        ticket.setEscalatedToUsername(managerUsername);
        ticket.setEscalatedBy(escalatedBy);
        ticket.setEscalatedByUsername(escalatedByUsername);
        ticket.setEscalationType(escalationType);
        ticket.setEscalationReason(request.getReason());
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setAssignedToUserId(managerId);
        ticket.setAssignedToUsername(managerUsername);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        TicketEscalatedEvent event = new TicketEscalatedEvent();
        event.setTicketId(savedTicket.getTicketId());
        event.setTicketNumber(savedTicket.getTicketNumber());
        event.setTitle(savedTicket.getTitle());
        event.setCategory(savedTicket.getCategory() != null ? savedTicket.getCategory().name() : null);
        event.setPriority(savedTicket.getPriority() != null ? savedTicket.getPriority().name() : null);
        event.setEscalationType(escalationType.name());
        event.setEscalationReason(request.getReason());
        event.setEscalatedBy(escalatedBy);
        event.setEscalatedByUsername(escalatedByUsername);
        event.setEscalatedToUserId(managerId);
        event.setEscalatedToUsername(managerUsername);
        event.setEscalatedToEmail(manager.getEmail());
        event.setPreviousAgentId(previousAgentId);
        event.setPreviousAgentUsername(previousAgentUsername);
        event.setEscalatedAt(savedTicket.getEscalatedAt());
        
        eventPublisherService.publishTicketEscalated(event);
        
        log.info("Ticket {} escalated to manager: {}", savedTicket.getTicketNumber(), managerUsername);
        
        return savedTicket;
    }
}
