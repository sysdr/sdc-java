package com.example.schemaregistry.exception;

public class IncompatibleSchemaException extends RuntimeException {
    
    public IncompatibleSchemaException(String message) {
        super(message);
    }
}
