package com.example.schemaregistry.dto;

import com.example.schemaregistry.entity.SchemaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompatibilityCheckRequest {
    
    @NotBlank(message = "Schema is required")
    private String schema;
    
    @NotNull(message = "Schema type is required")
    private SchemaType schemaType;
    
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    
    public SchemaType getSchemaType() { return schemaType; }
    public void setSchemaType(SchemaType schemaType) { this.schemaType = schemaType; }
}
