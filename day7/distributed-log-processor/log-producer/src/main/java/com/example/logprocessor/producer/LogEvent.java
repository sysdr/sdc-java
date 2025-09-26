package com.example.logprocessor.producer;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

public class LogEvent {
    @NotNull
    private final String id;
    @NotBlank
    private final String level;
    @NotBlank
    private final String message;
    @NotBlank
    private final String source;
    @NotNull
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    
    public LogEvent(String id, String level, String message, String source, LocalDateTime timestamp, Map<String, Object> metadata) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.level = level;
        this.message = message;
        this.source = source;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.metadata = metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public String getLevel() {
        return level;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getSource() {
        return source;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public static LogEvent create(String level, String message, String source, Map<String, Object> metadata) {
        return new LogEvent(
                UUID.randomUUID().toString(),
                level,
                message,
                source,
                LocalDateTime.now(),
                metadata
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEvent logEvent = (LogEvent) o;
        return Objects.equals(id, logEvent.id) &&
               Objects.equals(level, logEvent.level) &&
               Objects.equals(message, logEvent.message) &&
               Objects.equals(source, logEvent.source) &&
               Objects.equals(timestamp, logEvent.timestamp) &&
               Objects.equals(metadata, logEvent.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, level, message, source, timestamp, metadata);
    }
    
    @Override
    public String toString() {
        return "LogEvent{" +
               "id='" + id + '\'' +
               ", level='" + level + '\'' +
               ", message='" + message + '\'' +
               ", source='" + source + '\'' +
               ", timestamp=" + timestamp +
               ", metadata=" + metadata +
               '}';
    }
}
