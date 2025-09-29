package com.example.logprocessor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "log_events")
public class LogEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "correlation_id", nullable = false)
    private String correlationId;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "level", nullable = false)
    private String level;
    
    @Column(name = "source", nullable = false)
    private String source;
    
    @Column(name = "message", nullable = false, length = 1000)
    private String message;
    
    @ElementCollection
    @CollectionTable(name = "log_event_metadata", 
                    joinColumns = @JoinColumn(name = "log_event_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> metadata;
    
    @Column(name = "trace_id")
    private String traceId;

    // Constructors
    public LogEvent() {
        this.id = UUID.randomUUID();
        this.correlationId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public LogEvent(String level, String source, String message, Map<String, String> metadata) {
        this();
        this.level = level;
        this.source = source;
        this.message = message;
        this.metadata = metadata;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
