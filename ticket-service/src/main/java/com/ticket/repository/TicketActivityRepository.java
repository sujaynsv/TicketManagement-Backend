package com.ticket.repository;

import com.ticket.entity.TicketActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketActivityRepository extends MongoRepository<TicketActivity, String> {
    
    List<TicketActivity> findByTicketIdOrderByCreatedAtDesc(String ticketId);
}
