package com.ticket.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDeactivatedException extends RuntimeException{
    public AccountDeactivatedException(String message){
        super(message);
    }
}
