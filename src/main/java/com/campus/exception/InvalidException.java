package com.campus.exception;

public class InvalidException  extends  RuntimeException{
    public InvalidException(String msg)
    {
        super(msg);
    }
    public InvalidException(String msg,Throwable cause)
    {
        super(msg,cause);
    }
}
