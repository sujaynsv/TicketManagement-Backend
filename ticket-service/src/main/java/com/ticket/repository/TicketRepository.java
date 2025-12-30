package com.ticket.repository;

import com.ticket.entity.Ticket;
import com.ticket.enums.TicketStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    
    List<Ticket> findByCreatedByUserId(String userId);
    
    List<Ticket> findByAssignedToUserId(String userId);
    
    List<Ticket> findByStatus(TicketStatus status);
    
    List<Ticket> findByCreatedByUserIdAndStatus(String userId, TicketStatus status);
    
    List<Ticket> findByAssignedToUserIdAndStatus(String userId, TicketStatus status);
    
    Long countByStatus(TicketStatus status);
    
    Long countByCreatedByUserId(String userId);
    
    Long countByAssignedToUserId(String userId);
}
