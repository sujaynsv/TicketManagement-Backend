package com.ticket.dto;

public record TicketStatsDTO(
        long totalTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long escalatedTickets,
        long criticalTickets,
        long highPriorityTickets,
        long mediumPriorityTickets,
        long lowPriorityTickets,
        long noPriorityTickets
) {}
