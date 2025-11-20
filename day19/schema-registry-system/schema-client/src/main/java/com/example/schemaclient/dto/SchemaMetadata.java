package com.example.schemaclient.dto;

public class SchemaMetadata {
    private Long id;
    private String subject;
    private Integer version;
    private String schemaType;
    private String schema;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public String getSchemaType() { return schemaType; }
    public void setSchemaType(String schemaType) { this.schemaType = schemaType; }
    
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
