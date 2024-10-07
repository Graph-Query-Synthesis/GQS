package org.example.gqs.exceptions;

public class ResultMismatchException extends RuntimeException{

    private long index;

    public ResultMismatchException(String msg){
        super(msg);
    }
}
