package com.notification.client;

import com.notification.dto.TicketDTO;
import java.time.LocalDateTime;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketServiceClientFallback implements TicketServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(TicketServiceClientFallback.class);
    
    @Override
    public TicketDTO getTicket(String ticketId) {
        log.warn("Circuit breaker activated for getTicket({}). Using fallback.", ticketId);
        
        // Return fallback ticket data
            return new TicketDTO(
                ticketId,                      
                null,                           
                "Ticket Service Unavailable",  
                "Unable to fetch ticket details",
                "UNAVAILABLE",                  
                null,                           
                null,                          
                null,                           
                null,                         
                null,                           
                null,                          
                Collections.emptyList(),      
                0,                              
                0,                            
                LocalDateTime.now(),          
                null,                          
                null,                           
                null,                       
                null                           
            );
        
    }
}
