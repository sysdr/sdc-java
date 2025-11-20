package com.example.schemaregistry.dto;

import com.example.schemaregistry.entity.CompatibilityMode;

public class ConfigResponse {
    private CompatibilityMode compatibilityLevel;
    
    public ConfigResponse() {}
    public ConfigResponse(CompatibilityMode compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }
    
    public CompatibilityMode getCompatibilityLevel() { return compatibilityLevel; }
    public void setCompatibilityLevel(CompatibilityMode compatibilityLevel) { 
        this.compatibilityLevel = compatibilityLevel; 
    }
}
