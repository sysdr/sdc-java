package com.example.consumer;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_correlation_id", columnList = "correlationId"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class LogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String correlationId;
    
    @Column(nullable = false)
    private String severity;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String source;
    
    @Column(nullable = false)
    private long timestamp;
    
    private long processedAt;
    
    // Manual getters/setters if Lombok fails
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getProcessedAt() { return processedAt; }
    public void setProcessedAt(long processedAt) { this.processedAt = processedAt; }
}
