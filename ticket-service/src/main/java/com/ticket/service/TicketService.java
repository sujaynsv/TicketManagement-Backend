package com.ticket.service;

import com.ticket.dto.*;
import com.ticket.entity.Ticket;
import com.ticket.entity.TicketActivity;
import com.ticket.enums.TicketCategory;
import com.ticket.enums.TicketPriority;
import com.ticket.enums.TicketStatus;
import com.ticket.event.TicketCreatedEvent;
import com.ticket.event.TicketStatusChangedEvent;
import com.ticket.repository.CommentRepository;
import com.ticket.repository.TicketActivityRepository;
import com.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.events.CommentEvent;

import com.ticket.entity.Comment;
import com.ticket.event.CommentAddedEvent;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketService {
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketActivityRepository ticketActivityRepository;
    
    @Autowired
    private EventPublisherService eventPublisher;

    @Autowired
    private CommentRepository commentRepository;
    
    /**
     * Create a new ticket
     */
    // @Transactional
    // public TicketDTO createTicket(CreateTicketRequest request, String userId, String username) {
    //     // Parse enums
    //     TicketCategory category = TicketCategory.fromString(request.getCategory());
    //     TicketPriority priority = TicketPriority.fromString(request.getPriority());
        
    //     // Create ticket
    //     Ticket ticket = new Ticket();
    //     ticket.setTicketNumber(generateTicketNumber());
    //     ticket.setTitle(request.getTitle());
    //     ticket.setDescription(request.getDescription());
    //     ticket.setStatus(TicketStatus.OPEN);
    //     ticket.setCategory(category);
    //     ticket.setPriority(priority);
    //     ticket.setCreatedByUserId(userId);
    //     ticket.setCreatedByUsername(username);
    //     ticket.setTags(request.getTags() != null ? request.getTags() : List.of());
    //     ticket.setCreatedAt(LocalDateTime.now());
    //     ticket.setUpdatedAt(LocalDateTime.now());
    //     ticket.setCommentCount(0);
    //     ticket.setAttachmentCount(0);
        
    //     // Save ticket
    //     Ticket savedTicket = ticketRepository.save(ticket);
        
    //     // Log activity
    //     logActivity(savedTicket.getTicketId(), "TICKET_CREATED", 
    //                "Ticket created", userId, username);
        
    //     // Publish event to RabbitMQ
    //     TicketCreatedEvent event = new TicketCreatedEvent(
    //             savedTicket.getTicketId(),
    //             savedTicket.getTicketNumber(),
    //             savedTicket.getTitle(),
    //             userId,
    //             username,
    //             category.name(),
    //             priority.name(),
    //             savedTicket.getCreatedAt()
    //     );
    //     eventPublisher.publishTicketCreated(event);
        
    //     return convertToDTO(savedTicket);
    // }
    
    /**
     * Get ticket by ID
     */
    public TicketDTO getTicketById(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return convertToDTO(ticket);
    }
    
    /**
     * Get ticket by ticket number
     */
    public TicketDTO getTicketByNumber(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return convertToDTO(ticket);
    }
    
    /**
     * Update ticket
     */
    @Transactional
    public TicketDTO updateTicket(String ticketId, UpdateTicketRequest request, 
                                  String userId, String username) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Update fields if provided
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.category() != null) {
            ticket.setCategory(TicketCategory.fromString(request.category()));
        }
        if (request.priority() != null) {
            ticket.setPriority(TicketPriority.fromString(request.priority()));
        }
        if (request.tags() != null) {
            ticket.setTags(request.tags());
        }
        
        ticket.setUpdatedAt(LocalDateTime.now());
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        // Log activity
        logActivity(ticketId, "TICKET_UPDATED", "Ticket updated", userId, username);
        
        return convertToDTO(updatedTicket);
    }
    
    /**
     * Change ticket status
     */
    @Transactional
    public TicketDTO changeStatus(String ticketId, ChangeStatusRequest request, 
                                  String userId, String username) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = TicketStatus.valueOf(request.status().toUpperCase());
        
        // Validate status transition
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new RuntimeException("Cannot transition from " + oldStatus + " to " + newStatus);
        }
        
        // Update status
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        // Update timestamps based on status
        if (newStatus == TicketStatus.ASSIGNED && ticket.getAssignedAt() == null) {
            ticket.setAssignedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        
        Ticket updatedTicket = ticketRepository.save(ticket);

        if(request.comment()!=null && !request.comment().trim().isEmpty()){
            Comment comment=new Comment();
            comment.setTicketId(ticketId);
            comment.setCommentText(request.comment());
            comment.setUserId(userId);
            comment.setUsername(username);
            comment.setIsInternal(false);
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());

            Comment savedComment= commentRepository.save(comment);

            updatedTicket.setCommentCount(ticket.getCommentCount()+1);
            ticketRepository.save(updatedTicket);

            CommentAddedEvent commentEvent= new CommentAddedEvent(
                savedComment.getCommentId(),
                ticketId,
                ticket.getTicketNumber(),
                userId,
                username,
                request.comment(),
                false,
                LocalDateTime.now()
            );
            eventPublisher.publishCommentAdded(commentEvent);

        }
        
        // Log activity
        TicketActivity activity = new TicketActivity(
                ticketId,
                "STATUS_CHANGED",
                "Status changed from " + oldStatus + " to " + newStatus,
                userId,
                username
        );
        activity.setOldValue(oldStatus.name());
        activity.setNewValue(newStatus.name());
        ticketActivityRepository.save(activity);
        
        // Publish event
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                ticketId,
                ticket.getTicketNumber(),
                oldStatus.name(),
                newStatus.name(),
                userId,
                username,
                request.comment(),
                LocalDateTime.now()
        );
        eventPublisher.publishTicketStatusChanged(event);
        
        return convertToDTO(updatedTicket);
    }
    
    /**
     * Get all tickets for a user
     */
    public List<TicketDTO> getMyTickets(String userId) {
        List<Ticket> tickets = ticketRepository.findByCreatedByUserId(userId);
        return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get tickets assigned to user
     */
    public List<TicketDTO> getAssignedTickets(String userId) {
        List<Ticket> tickets = ticketRepository.findByAssignedToUserId(userId);
        return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get tickets by status
     */
    public List<TicketDTO> getTicketsByStatus(String status) {
        TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
        List<Ticket> tickets = ticketRepository.findByStatus(ticketStatus);
        return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Get all tickets
     */
    public List<TicketDTO> getAllTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * Delete ticket (admin only)
     */
    @Transactional
    public void deleteTicket(String ticketId) {
        ticketRepository.deleteById(ticketId);
    }
    
    /**
     * Increment comment count
     */
    public void incrementCommentCount(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setCommentCount(ticket.getCommentCount() + 1);
        ticketRepository.save(ticket);
    }
    
    /**
     * Increment attachment count
     */
    public void incrementAttachmentCount(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAttachmentCount(ticket.getAttachmentCount() + 1);
        ticketRepository.save(ticket);
    }
    
    /**
     * Generate unique ticket number: TKT-YYYYMMDD-00001
     */
    private String generateTicketNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = ticketRepository.count() + 1;
        String sequencePart = String.format("%05d", count);
        return "TKT-" + datePart + "-" + sequencePart;
    }
    
    /**
     * Log activity
     */
    private void logActivity(String ticketId, String activityType, String description, 
                            String userId, String username) {
        TicketActivity activity = new TicketActivity(
                ticketId, activityType, description, userId, username
        );
        ticketActivityRepository.save(activity);
    }
    
    /**
     * Convert entity to DTO
     */
    private TicketDTO convertToDTO(Ticket ticket) {
        return new TicketDTO(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus().name(),
                ticket.getCategory().name(),
                ticket.getPriority().name(),
                ticket.getCreatedByUserId(),
                ticket.getCreatedByUsername(),
                ticket.getAssignedToUserId(),
                ticket.getAssignedToUsername(),
                ticket.getTags(),
                ticket.getCommentCount(),
                ticket.getAttachmentCount(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getAssignedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt()
        );
    }

    // @Autowired
    // private AttachmentService attachmentService;
    //created circular dependency issue for me.

    /**
     * Create ticket (without handling attachments here)
     * Attachments will be handled in the controller
     */
    @Transactional
    public TicketDTO createTicket(CreateTicketRequest request, String userId, String username) {
        // Parse enums
        TicketCategory category = TicketCategory.fromString(request.getCategory());
        TicketPriority priority = TicketPriority.fromString(request.getPriority());
        
        // Create ticket
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setCreatedByUserId(userId);
        ticket.setCreatedByUsername(username);
        ticket.setTags(request.getTags() != null ? request.getTags() : List.of());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setCommentCount(0);
        ticket.setAttachmentCount(0);
        
        // Save ticket
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Log activity
        logActivity(savedTicket.getTicketId(), "TICKET_CREATED", 
                "Ticket created", userId, username);
        
        // Publish event to RabbitMQ
        TicketCreatedEvent event = new TicketCreatedEvent(
                savedTicket.getTicketId(),
                savedTicket.getTicketNumber(),
                savedTicket.getTitle(),
                savedTicket.getDescription(),
                userId,
                username,
                category.name(),
                priority.name(),
                savedTicket.getCreatedAt()
        );
        eventPublisher.publishTicketCreated(event);
        
        return convertToDTO(savedTicket);
    }

    /**
     * Update attachment count
     */
    @Transactional
    public void updateAttachmentCount(String ticketId, int count) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAttachmentCount(count);
        ticketRepository.save(ticket);
    }



}
