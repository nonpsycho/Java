package com.gnomeland.foodlab.exception;

public class LogNotReadyException extends RuntimeException {
    public LogNotReadyException(String message) {
        super(message);
    }
}
