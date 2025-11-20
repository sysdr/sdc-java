package com.example.schemaregistry.dto;

public class RegisterSchemaResponse {
    private Long id;
    
    public RegisterSchemaResponse() {}
    public RegisterSchemaResponse(Long id) { this.id = id; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
