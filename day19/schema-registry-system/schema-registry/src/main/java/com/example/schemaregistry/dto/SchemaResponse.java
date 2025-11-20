package com.example.schemaregistry.dto;

import com.example.schemaregistry.entity.SchemaType;
import java.io.Serializable;

public class SchemaResponse implements Serializable {
    private Long id;
    private String subject;
    private Integer version;
    private SchemaType schemaType;
    private String schema;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public SchemaType getSchemaType() { return schemaType; }
    public void setSchemaType(SchemaType schemaType) { this.schemaType = schemaType; }
    
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
