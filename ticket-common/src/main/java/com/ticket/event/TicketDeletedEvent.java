package com.ticket.event;
import java.time.LocalDateTime;

public class TicketDeletedEvent {
    private String ticketId;
    private String ticketNumber;
    private LocalDateTime deletedAt;

    // Default Constructor
    public TicketDeletedEvent() {
    }

    // Parameterized Constructor
    public TicketDeletedEvent(String ticketId, String ticketNumber, LocalDateTime deletedAt) {
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.deletedAt = deletedAt;
    }

    // Getters and Setters
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}