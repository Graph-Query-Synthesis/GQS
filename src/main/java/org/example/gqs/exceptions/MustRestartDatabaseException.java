package org.example.gqs.exceptions;

public class MustRestartDatabaseException extends RuntimeException{

    public MustRestartDatabaseException(Exception cause){
        super(cause);
    }
}
