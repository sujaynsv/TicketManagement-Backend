package com.assignment.controller;

import com.assignment.dto.SlaTrackingDTO;
import com.assignment.entity.SlaStatus;
import com.assignment.entity.SlaTracking;
import com.assignment.repository.SlaTrackingRepository;
import com.assignment.service.SlaService;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sla")
public class SlaController {
    
    @Autowired
    private SlaService slaService;

    @Autowired
    private SlaTrackingRepository slaTrackingRepository;
    
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

    /**
     * Get all breached SLA's
     */
    @GetMapping("/breached")
    public ResponseEntity<List<SlaTrackingDTO>> getBreachedSlas(){
        List<SlaTracking> breached=slaTrackingRepository.findBySlaStatus(SlaStatus.BREACHED);
        List<SlaTrackingDTO> dtos=breached.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all active SLA trackings
     */
    @GetMapping("/active")
    public ResponseEntity<List<SlaTrackingDTO>> getActiveSlas(){
        List<SlaTracking> active = slaTrackingRepository.findByResolvedAtIsNull();
        List<SlaTrackingDTO> dtos=active.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get SLA warnings which are at risk
     */
    @GetMapping("/warnings")
    public ResponseEntity<List<SlaTrackingDTO>> getSlaWarnings(){
        List<SlaTracking> warnings=slaTrackingRepository.findBySlaStatus(SlaStatus.WARNING);
        List<SlaTrackingDTO> dtos=warnings.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
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
