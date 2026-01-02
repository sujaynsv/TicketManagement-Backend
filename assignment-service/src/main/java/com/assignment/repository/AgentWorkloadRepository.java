package com.assignment.repository;

import com.assignment.entity.AgentStatus;
import com.assignment.entity.AgentWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentWorkloadRepository extends JpaRepository<AgentWorkload, String> {
    
    // Find available agents sorted by workload (least loaded first)
    List<AgentWorkload> findByStatusOrderByActiveTicketsAsc(AgentStatus status);
    
    // Find all available agents
    List<AgentWorkload> findByStatusIn(List<AgentStatus> statuses);
    
    // Find agent with least workload
    @Query("SELECT a FROM AgentWorkload a WHERE a.status = 'AVAILABLE' ORDER BY a.activeTickets ASC LIMIT 1")
    AgentWorkload findAgentWithLeastWorkload();

    long countByStatus(AgentStatus status);

}
