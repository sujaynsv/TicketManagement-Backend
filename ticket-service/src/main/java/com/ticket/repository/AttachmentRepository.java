package com.ticket.repository;

import com.ticket.entity.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends MongoRepository<Attachment, String> {
    
    List<Attachment> findByTicketIdOrderByUploadedAtDesc(String ticketId);
    
    Long countByTicketId(String ticketId);
}
