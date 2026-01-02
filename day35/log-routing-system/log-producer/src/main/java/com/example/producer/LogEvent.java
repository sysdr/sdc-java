package com.example.producer;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class LogEvent {
    private String id;
    private String timestamp;
    private String severity;
    private String source;
    private String type;
    private String message;
    private Map<String, Object> metadata;
    
    public LogEvent() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
    }
}
