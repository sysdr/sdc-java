package com.example.logprocessor.producer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class LogEvent {
    private String id;
    private String organizationId;
    private String level;
    private String message;
    private String source;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public LogEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public LogEvent(String organizationId, String level, String message, String source) {
        this();
        this.organizationId = organizationId;
        this.level = level;
        this.message = message;
        this.source = source;
        this.id = java.util.UUID.randomUUID().toString();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
