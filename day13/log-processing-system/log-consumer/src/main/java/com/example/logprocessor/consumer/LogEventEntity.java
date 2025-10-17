package com.example.logprocessor.consumer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_source", columnList = "source")
})
public class LogEventEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String level;

    @Column(length = 4096)
    private String message;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public LogEventEntity() {}

    public LogEventEntity(String id, String level, String message, 
                         String source, Instant timestamp, String metadata) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.source = source;
        this.timestamp = timestamp;
        this.processedAt = Instant.now();
        this.metadata = metadata;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
