package com.assignment.repository;

import com.assignment.entity.Assignment;
import com.assignment.entity.AssignmentStatus;
import com.assignment.entity.AssignmentType;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String>, JpaSpecificationExecutor<Assignment> {
    
    Optional<Assignment> findByTicketIdAndStatus(String ticketId, AssignmentStatus status);
    List<Assignment> findByAgentIdAndStatus(String agentId, AssignmentStatus status);
    List<Assignment> findByTicketIdOrderByAssignedAtDesc(String ticketId);
    Long countByAgentIdAndStatus(String agentId, AssignmentStatus status);

    List<Assignment> findByAgentIdAndStatusOrderByAssignedAtDesc(String agentId, AssignmentStatus status);
    
    // New methods for admin
    long countByStatus(AssignmentStatus status);
    long countByAssignmentType(AssignmentType type);
    long countByAgentId(String agentId);
    long countByAgentIdAndStatusNot(String agentId, AssignmentStatus status);

        // Add these methods
    long countByAssignedAtAfter(LocalDateTime after);
    long countByAssignedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Assignment> findByAgentIdAndCompletedAtIsNotNull(String agentId);
    List<Assignment> findByAgentId(String agentId);

    List<Assignment> findByAgentIdOrderByAssignedAtDesc(String agentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Assignment a WHERE a.ticketId = :ticketId")
    int deleteByTicketId(String ticketId);



}
