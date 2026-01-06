package com.assignment.exception;

public class AuthServiceClientException extends RuntimeException{

    public AuthServiceClientException(String message, Exception ex){
        super(message, ex);
    }
    
}
