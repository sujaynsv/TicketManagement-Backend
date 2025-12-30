package com.ticket.repository;

import com.ticket.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    List<Comment> findByTicketIdOrderByCreatedAtDesc(String ticketId);
    
    List<Comment> findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(String ticketId);
    
    Long countByTicketId(String ticketId);
}
