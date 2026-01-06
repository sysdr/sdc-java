package com.example.kafka.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Map;

public class LogEvent {
    private String eventId;
    private String serviceName;
    private String severity;
    private String message;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    private Map<String, String> metadata;
    private String environment;
    private String hostName;

    public LogEvent() {
        this.timestamp = Instant.now();
    }

    public LogEvent(String serviceName, String severity, String message) {
        this();
        this.serviceName = serviceName;
        this.severity = severity;
        this.message = message;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    @Override
    public String toString() {
        return "LogEvent{" +
                "eventId='" + eventId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", severity='" + severity + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
