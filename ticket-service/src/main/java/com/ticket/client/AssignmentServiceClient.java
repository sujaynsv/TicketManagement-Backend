package com.ticket.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ticket.dto.AssignmentDTO;

@FeignClient(
    name = "assignment-service",
    url = "${services.assignment-service.url:http://localhost:8083}",
    fallback = AssignmentServiceClientFallback.class
)
public interface AssignmentServiceClient {
    
    @GetMapping("/assignments/ticket/{ticketId}")
    AssignmentDTO getAssignmentByTicketId(@PathVariable("ticketId") String ticketId);
}
