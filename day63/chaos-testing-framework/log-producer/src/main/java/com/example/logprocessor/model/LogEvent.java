package com.example.logprocessor.model;

import java.time.Instant;
import java.util.UUID;

public class LogEvent {
    private String id;
    private String level;
    private String message;
    private String source;
    private Instant timestamp;

    public LogEvent() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public LogEvent(String level, String message, String source) {
        this();
        this.level = level;
        this.message = message;
        this.source = source;
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
}
