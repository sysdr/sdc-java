package com.example.logprocessor.producer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public class LogEvent {
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("level")
    private String level;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("trace_id")
    private String traceId;

    // Constructors
    public LogEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public LogEvent(String level, String source, String message, Map<String, Object> metadata) {
        this();
        this.level = level;
        this.source = source;
        this.message = message;
        this.metadata = metadata;
    }

    // Getters and setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Override
    public String toString() {
        return "LogEvent{" +
                "timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", source='" + source + '\'' +
                ", message='" + message + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
