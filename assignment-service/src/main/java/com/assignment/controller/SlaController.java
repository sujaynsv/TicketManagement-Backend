package com.assignment.controller;

import com.assignment.dto.SlaTrackingDTO;
import com.assignment.entity.SlaTracking;
import com.assignment.service.SlaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sla")
public class SlaController {
    
    @Autowired
    private SlaService slaService;
    
    /**
     * Get SLA tracking for a ticket
     */
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<SlaTrackingDTO> getSlaTracking(@PathVariable String ticketId) {
        SlaTracking tracking = slaService.getSlaTracking(ticketId)
                .orElseThrow(() -> new RuntimeException("SLA tracking not found for ticket"));
        
        SlaTrackingDTO dto = convertToDTO(tracking);
        return ResponseEntity.ok(dto);
    }
    
    private SlaTrackingDTO convertToDTO(SlaTracking tracking) {
        SlaTrackingDTO dto = new SlaTrackingDTO();
        dto.setTrackingId(tracking.getTrackingId());
        dto.setTicketId(tracking.getTicketId());
        dto.setTicketNumber(tracking.getTicketNumber());
        dto.setPriority(tracking.getPriority());
        dto.setCategory(tracking.getCategory());
        dto.setResponseDueAt(tracking.getResponseDueAt());
        dto.setResolutionDueAt(tracking.getResolutionDueAt());
        dto.setFirstResponseAt(tracking.getFirstResponseAt());
        dto.setResponseBreached(tracking.getResponseBreached());
        dto.setResponseTimeMinutes(tracking.getResponseTimeMinutes());
        dto.setResolvedAt(tracking.getResolvedAt());
        dto.setResolutionBreached(tracking.getResolutionBreached());
        dto.setResolutionTimeHours(tracking.getResolutionTimeHours());
        dto.setSlaStatus(tracking.getSlaStatus().name());
        dto.setTimeRemaining(slaService.calculateTimeRemaining(tracking));
        return dto;
    }
}
