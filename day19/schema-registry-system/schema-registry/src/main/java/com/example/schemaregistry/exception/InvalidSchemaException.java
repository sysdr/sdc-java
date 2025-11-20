package com.example.schemaregistry.exception;

public class InvalidSchemaException extends RuntimeException {
    
    public InvalidSchemaException(String message) {
        super(message);
    }
    
    public InvalidSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
