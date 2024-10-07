package org.example.gqs.exceptions;

public class DatabaseCrashException extends RuntimeException{

    private long index;

    public DatabaseCrashException(Exception e, long index){
        super("database "+index+" crashed",e);
    }
}
