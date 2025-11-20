package com.example.schemaregistry.dto;

import jakarta.validation.constraints.NotNull;

public class ValidationRequest {
    
    @NotNull(message = "Payload is required")
    private byte[] payload;
    
    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] payload) { this.payload = payload; }
}
