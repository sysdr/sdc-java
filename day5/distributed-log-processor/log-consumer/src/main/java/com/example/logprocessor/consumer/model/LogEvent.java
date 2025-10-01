package com.example.logprocessor.consumer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_trace_id", columnList = "trace_id")
})
public class LogEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false, length = 20)
    private String level;
    
    @Column(nullable = false, length = 100)
    private String source;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "trace_id", length = 100)
    private String traceId;
    
    @ElementCollection
    @CollectionTable(name = "log_metadata", 
                    joinColumns = @JoinColumn(name = "log_event_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;

    // Constructors
    public LogEvent() {}

    public LogEvent(LocalDateTime timestamp, String level, String source, String message, String traceId) {
        this.timestamp = timestamp;
        this.level = level;
        this.source = source;
        this.message = message;
        this.traceId = traceId;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
