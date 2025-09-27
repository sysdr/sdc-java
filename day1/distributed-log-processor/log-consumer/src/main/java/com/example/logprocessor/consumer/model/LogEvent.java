package com.example.logprocessor.consumer.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_events")
public class LogEvent {
    
    @Id
    private String id;
    
    @Column(name = "organization_id", nullable = false)
    private String organizationId;
    
    @Column(name = "level", nullable = false)
    private String level;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "source")
    private String source;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public LogEvent() {}

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

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
