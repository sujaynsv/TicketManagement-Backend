package com.assignment.exception;

public class TicketNotAssignedException extends RuntimeException{
    public TicketNotAssignedException(String message){
        super(message);
    }   
}
