package com.assignment.repository;

import com.assignment.entity.Assignment;
import com.assignment.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    
    // Find by ticket ID
    Optional<Assignment> findByTicketIdAndStatus(String ticketId, AssignmentStatus status);
    
    // Find by agent ID
    List<Assignment> findByAgentIdAndStatus(String agentId, AssignmentStatus status);
    
    // Find all assignments for a ticket
    List<Assignment> findByTicketIdOrderByAssignedAtDesc(String ticketId);
    
    // Count active assignments for agent
    Long countByAgentIdAndStatus(String agentId, AssignmentStatus status);

}
