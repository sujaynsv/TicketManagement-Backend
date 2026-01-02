package com.assignment.repository;

import com.assignment.entity.TicketCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCacheRepository extends JpaRepository<TicketCache, String> {
    
    // Find unassigned tickets
    List<TicketCache> findByStatusAndAssignedAgentIdIsNull(String status);
    
    // Find tickets by agent
    List<TicketCache> findByAssignedAgentId(String agentId);
    
    // Find by ticket number
    Optional<TicketCache> findByTicketNumber(String ticketNumber);
    
    // Find by status
    List<TicketCache> findByStatus(String status);

    
    Optional<TicketCache> findByTicketId(String ticketId);

    
    // Count unassigned tickets
    Long countByStatusAndAssignedAgentIdIsNull(String status);

        // Add these methods
    long countByStatus(String status);
    long countByPriority(String priority);
    long countByPriorityIsNull();
    long countByPriorityAndStatusNot(String priority, String status);
    long countByStatusAndUpdatedAtAfter(String status, LocalDateTime after);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndUpdatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

}
