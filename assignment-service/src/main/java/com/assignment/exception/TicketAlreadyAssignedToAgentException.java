package com.assignment.exception;

public class TicketAlreadyAssignedToAgentException extends RuntimeException{
    public TicketAlreadyAssignedToAgentException(String message){
        super(message);
    }
    
}
