package com.example.schemaregistry.dto;

import com.example.schemaregistry.entity.CompatibilityMode;
import jakarta.validation.constraints.NotNull;

public class ConfigUpdateRequest {
    
    @NotNull(message = "Compatibility level is required")
    private CompatibilityMode compatibility;
    
    public CompatibilityMode getCompatibility() { return compatibility; }
    public void setCompatibility(CompatibilityMode compatibility) { 
        this.compatibility = compatibility; 
    }
}
