package com.ticket.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketPriorityUpdatedEvent implements Serializable {
    private String ticketId;
    private String ticketNumber;
    private String oldPriority;  // Can be null
    private String newPriority;  // CRITICAL, HIGH, MEDIUM, LOW
    private String updatedBy;
    private String updatedByUsername;
    private String reason;
    private LocalDateTime updatedAt;
}
