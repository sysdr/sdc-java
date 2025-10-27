package com.example.logprocessor.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

public class LogEvent {
    
    @NotNull(message = "Log level is required")
    @Pattern(regexp = "^(DEBUG|INFO|WARN|ERROR|FATAL)$", 
             message = "Log level must be DEBUG, INFO, WARN, ERROR, or FATAL")
    @JsonProperty("level")
    private String level;
    
    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @NotBlank(message = "Message is required")
    @Size(max = 10000, message = "Message cannot exceed 10000 characters")
    @JsonProperty("message")
    private String message;
    
    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name cannot exceed 100 characters")
    @JsonProperty("service")
    private String service;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("traceId")
    private String traceId;
    
    @JsonProperty("spanId")
    private String spanId;

    // Constructors
    public LogEvent() {
        this.timestamp = Instant.now();
    }

    public LogEvent(String level, String message, String service) {
        this.level = level;
        this.message = message;
        this.service = service;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    @Override
    public String toString() {
        return String.format("LogEvent{level='%s', timestamp=%s, service='%s', message='%s'}", 
                           level, timestamp, service, message);
    }
}
