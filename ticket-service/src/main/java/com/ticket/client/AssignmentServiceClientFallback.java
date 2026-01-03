package com.ticket.client;

import com.ticket.dto.AssignmentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssignmentServiceClientFallback implements AssignmentServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(AssignmentServiceClientFallback.class);
    
    @Override
    public AssignmentDTO getAssignmentByTicketId(String ticketId) {
        log.warn("Circuit breaker activated for getAssignmentByTicketId({}). Using fallback.", ticketId);
        
        // Return fallback assignment data
        AssignmentDTO fallbackAssignment = new AssignmentDTO();
        fallbackAssignment.setTicketId(ticketId);
        fallbackAssignment.setStatus("UNAVAILABLE");
        
        return fallbackAssignment;
    }
}
