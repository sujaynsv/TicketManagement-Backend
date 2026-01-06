package com.ticket.exception;

public class AttachmentStorageException extends RuntimeException{
    public AttachmentStorageException(String message, Exception ex){
        super(message,ex);
    }
    
}
