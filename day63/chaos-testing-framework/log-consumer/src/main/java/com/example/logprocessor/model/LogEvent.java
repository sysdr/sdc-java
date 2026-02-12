package com.example.logprocessor.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "log_events")
public class LogEvent {
    @Id
    private String id;
    
    private String level;
    
    @Column(length = 2000)
    private String message;
    
    private String source;
    
    private Instant timestamp;
    
    private Instant processedAt;

    // Constructors
    public LogEvent() {}

    public LogEvent(String id, String level, String message, String source, Instant timestamp) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.source = source;
        this.timestamp = timestamp;
        this.processedAt = Instant.now();
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
}
