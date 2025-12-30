package com.ticket.controller;

import com.ticket.dto.TicketActivityDTO;
import com.ticket.entity.TicketActivity;
import com.ticket.repository.TicketActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets/{ticketId}/activities")
public class TicketActivityController {
    
    @Autowired
    private TicketActivityRepository ticketActivityRepository;
    
    /**
     * Get activity log for ticket
     */
    @GetMapping
    public ResponseEntity<List<TicketActivityDTO>> getActivities(@PathVariable String ticketId) {
        List<TicketActivity> activities = ticketActivityRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        
        List<TicketActivityDTO> activityDTOs = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(activityDTOs);
    }
    
    /**
     * Convert entity to DTO
     */
    private TicketActivityDTO convertToDTO(TicketActivity activity) {
        return new TicketActivityDTO(
                activity.getActivityId(),
                activity.getTicketId(),
                activity.getActivityType(),
                activity.getDescription(),
                activity.getPerformedByUserId(),
                activity.getPerformedByUsername(),
                activity.getOldValue(),
                activity.getNewValue(),
                activity.getCreatedAt()
        );
    }
}
