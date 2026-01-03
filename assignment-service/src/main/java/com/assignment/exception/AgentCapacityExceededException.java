package com.assignment.exception;

public class AgentCapacityExceededException extends RuntimeException{
    public AgentCapacityExceededException(String message){
        super(message);
    }
}
