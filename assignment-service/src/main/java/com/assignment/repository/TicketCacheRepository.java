package com.assignment.repository;

import com.assignment.entity.TicketCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
