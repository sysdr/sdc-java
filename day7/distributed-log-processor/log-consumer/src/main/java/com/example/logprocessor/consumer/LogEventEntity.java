package com.example.logprocessor.consumer;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "log_events", indexes = {
        @Index(name = "idx_log_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_level", columnList = "level"),
        @Index(name = "idx_log_source", columnList = "source"),
        @Index(name = "idx_log_timestamp_level", columnList = "timestamp, level")
})
public class LogEventEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false, length = 10)
    private String level;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false, length = 100)
    private String source;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(type = "json")
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public LogEventEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public LogEventEntity(String id, String level, String message, String source, 
                         LocalDateTime timestamp, Map<String, Object> metadata) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.source = source;
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
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
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
