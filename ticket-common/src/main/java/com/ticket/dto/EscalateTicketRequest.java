package com.ticket.dto;

public class EscalateTicketRequest {
    
    private String reason;
    
    public EscalateTicketRequest() {}
    
    public EscalateTicketRequest(String reason) {
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
