package com.ticket.exception;

public class AttachmentFetchException extends RuntimeException{
    public AttachmentFetchException(String message, Exception ex){
        super(message,ex);
    }
    
}
