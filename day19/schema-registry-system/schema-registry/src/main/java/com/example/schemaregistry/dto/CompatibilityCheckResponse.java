package com.example.schemaregistry.dto;

public class CompatibilityCheckResponse {
    private boolean compatible;
    private String message;
    
    public CompatibilityCheckResponse() {}
    
    public CompatibilityCheckResponse(boolean compatible, String message) {
        this.compatible = compatible;
        this.message = message;
    }
    
    public static CompatibilityCheckResponse compatible() {
        return new CompatibilityCheckResponse(true, "Schema is compatible");
    }
    
    public static CompatibilityCheckResponse incompatible(String reason) {
        return new CompatibilityCheckResponse(false, reason);
    }
    
    public boolean isCompatible() { return compatible; }
    public void setCompatible(boolean compatible) { this.compatible = compatible; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
