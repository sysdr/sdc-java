package com.example.logprocessor.consumer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_service_timestamp", columnList = "service,timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String level;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false, length = 100)
    private String service;
    
    @Column(columnDefinition = "JSONB")
    private String metadata;
    
    @Column(length = 32)
    private String traceId;
    
    @Column(length = 16)
    private String spanId;
    
    @Column(nullable = false)
    private Instant processedAt;

    // Constructors
    public LogEntry() {
        this.processedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
