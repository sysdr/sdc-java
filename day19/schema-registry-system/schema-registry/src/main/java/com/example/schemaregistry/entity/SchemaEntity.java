package com.example.schemaregistry.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "schemas", indexes = {
    @Index(name = "idx_subject_version", columnList = "subject, version", unique = true),
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_subject", columnList = "subject")
})
public class SchemaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "schema_id_seq")
    @SequenceGenerator(name = "schema_id_seq", sequenceName = "schema_id_seq", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String subject;
    
    @Column(nullable = false)
    private Integer version;
    
    @Column(name = "schema_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SchemaType schemaType;
    
    @Column(name = "schema_definition", nullable = false, columnDefinition = "TEXT")
    private String schema;
    
    @Column(nullable = false, length = 64)
    private String fingerprint;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", length = 255)
    private String createdBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
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
    
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
