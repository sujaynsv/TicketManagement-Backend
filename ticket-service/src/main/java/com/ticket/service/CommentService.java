package com.ticket.service;

import com.ticket.dto.CommentDTO;
import com.ticket.dto.CreateCommentRequest;
import com.ticket.entity.Comment;
import com.ticket.entity.Ticket;
import com.ticket.event.CommentAddedEvent;
import com.ticket.repository.CommentRepository;
import com.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    
    private CommentRepository commentRepository;
    
    private TicketRepository ticketRepository;
    
    private TicketService ticketService;
    
    private EventPublisherService eventPublisherService;

    public CommentService(EventPublisherService eventPublisherService, TicketService ticketService, TicketRepository ticketRepository, CommentRepository commentRepository){
        this.eventPublisherService=eventPublisherService;
        this.ticketService=ticketService;
        this.ticketRepository=ticketRepository;
        this.commentRepository=commentRepository;
    }
    
    
    /**
     * Add comment to ticket
     */
    @Transactional
    public CommentDTO addComment(String ticketId, CreateCommentRequest request, 
                                String userId, String username) {
        // Verify ticket exists
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Create comment
        Comment comment = new Comment();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setUsername(username);
        comment.setCommentText(request.commentText());
        comment.setIsInternal(Boolean.TRUE.equals(request.isInternal()));
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment savedComment = commentRepository.save(comment);
        
        // Increment ticket comment count
        ticketService.incrementCommentCount(ticketId);
        
        // Publish event
        CommentAddedEvent event = new CommentAddedEvent(
                savedComment.getCommentId(),
                ticketId,
                ticket.getTicketNumber(),
                userId,
                username,
                request.commentText(),
                savedComment.getIsInternal(),
                savedComment.getCreatedAt()
        );
        eventPublisherService.publishCommentAdded(event);
        
        return convertToDTO(savedComment);
    }
    
    /**
     * Get all comments for ticket
     */
    public List<CommentDTO> getCommentsByTicket(String ticketId, boolean includeInternal) {
        List<Comment> comments;
        if (includeInternal) {
            comments = commentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        } else {
            comments = commentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(ticketId);
        }
        return comments.stream().map(this::convertToDTO).toList();
    }
    
    /**
     * Convert entity to DTO
     */
    private CommentDTO convertToDTO(Comment comment) {
        return new CommentDTO(
                comment.getCommentId(),
                comment.getTicketId(),
                comment.getUserId(),
                comment.getUsername(),
                comment.getCommentText(),
                comment.getIsInternal(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
