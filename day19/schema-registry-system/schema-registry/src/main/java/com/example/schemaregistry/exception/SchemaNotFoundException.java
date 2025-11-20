package com.example.schemaregistry.exception;

public class SchemaNotFoundException extends RuntimeException {
    
    public SchemaNotFoundException(Long id) {
        super("Schema not found with ID: " + id);
    }
    
    public SchemaNotFoundException(String subject, Integer version) {
        super("Schema not found: " + subject + " version " + version);
    }
    
    public SchemaNotFoundException(String message) {
        super(message);
    }
}
