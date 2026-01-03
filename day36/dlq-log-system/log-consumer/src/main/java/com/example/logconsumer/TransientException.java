package com.example.logconsumer;

public class TransientException extends RuntimeException {
    public TransientException(String message) {
        super(message);
    }
}
