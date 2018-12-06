package com.campus.exception;

public class ServeErrorException extends RuntimeException{
    public ServeErrorException(String message) {
        super(message);
    }

    public ServeErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
