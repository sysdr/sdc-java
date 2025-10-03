package com.example.logprocessor.gateway;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_log_timestamp_level", columnList = "timestamp DESC, logLevel"),
    @Index(name = "idx_log_source", columnList = "source"),
    @Index(name = "idx_log_errors", columnList = "timestamp DESC", unique = false)
})
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 2048)
    private String message;
    
    @Column(name = "log_level", nullable = false, length = 10)
    private String logLevel;
    
    @Column(nullable = false, length = 255)
    private String source;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @ElementCollection
    @CollectionTable(name = "log_metadata", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 1024)
    private Map<String, String> metadata;

    // Constructors
    public LogEntry() {}

    public LogEntry(String message, String logLevel, String source, Instant timestamp, Map<String, String> metadata) {
        this.message = message;
        this.logLevel = logLevel;
        this.source = source;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
