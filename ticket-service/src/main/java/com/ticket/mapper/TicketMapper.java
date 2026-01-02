package com.ticket.mapper;

import com.ticket.dto.TicketDTO;
import com.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    
    public TicketDTO toDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        
        return new TicketDTO(
            ticket.getTicketId(),
            ticket.getTicketNumber(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getStatus() != null ? ticket.getStatus().name() : null,
            ticket.getCategory() != null ? ticket.getCategory().name() : null,
            ticket.getPriority() != null ? ticket.getPriority().name() : null,
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
}
