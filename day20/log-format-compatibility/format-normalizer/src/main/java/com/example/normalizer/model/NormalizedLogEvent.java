package com.example.normalizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NormalizedLogEvent {
    private String id;
    private Instant timestamp;
    private String level; // DEBUG, INFO, WARN, ERROR, CRITICAL
    private String source; // syslog, journald, application
    private String hostname;
    private String application;
    private String message;
    private Map<String, Object> metadata;
    private Map<String, Object> rawFormat; // Original format-specific data

    // Constructors
    public NormalizedLogEvent() {}

    public NormalizedLogEvent(String source, String level, String message) {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.source = source;
        this.level = level;
        this.message = message;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Map<String, Object> getRawFormat() { return rawFormat; }
    public void setRawFormat(Map<String, Object> rawFormat) { this.rawFormat = rawFormat; }
}
