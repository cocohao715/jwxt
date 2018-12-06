package com.campus.exception;

public class RepeatBindException extends RuntimeException {
    public RepeatBindException(String message) {
        super(message);
    }

    public RepeatBindException(String message, Throwable cause) {
        super(message, cause);
    }
}
