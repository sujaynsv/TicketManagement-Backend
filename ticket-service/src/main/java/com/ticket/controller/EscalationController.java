package com.ticket.controller;

import com.ticket.dto.EscalateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.entity.Ticket;
import com.ticket.enums.EscalationType;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
public class EscalationController {
    
    private final EscalationService escalationService;
    private final TicketMapper ticketMapper;
    
    @PostMapping("/{ticketId}/escalate")
    public ResponseEntity<TicketDTO> escalateTicket(
            @PathVariable String ticketId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username,
            @RequestBody EscalateTicketRequest request) {
    
        log.info("Escalation request for ticket {} by user {}", ticketId, username);
        
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Ticket escalatedTicket = escalationService.escalateTicket(
            ticketId, 
            userId, 
            username, 
            request, 
            EscalationType.MANUAL
        );
        
        return ResponseEntity.ok(ticketMapper.toDTO(escalatedTicket));
    }
}
