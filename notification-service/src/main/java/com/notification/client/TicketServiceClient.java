package com.notification.client;

import com.notification.dto.TicketDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "ticket-service",
    url = "${services.ticket-service.url:http://localhost:8082}",
    fallback = TicketServiceClientFallback.class
)
public interface TicketServiceClient {
    
    @GetMapping("/tickets/{ticketId}")
    TicketDTO getTicket(@PathVariable("ticketId") String ticketId);
}
