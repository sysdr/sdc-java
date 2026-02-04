package com.example.logprocessor.consumer.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for persistent log storage.
 * Index on (source, timestamp) supports the most common query pattern:
 * "show me logs from service X in the last N minutes."
 */
@Entity
@Table(
    name = "log_events",
    indexes = {
        @Index(name = "idx_source_timestamp", columnList = "source, timestamp")
    }
)
public class LogEventEntity {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "source", nullable = false, length = 128)
    private String source;

    @Column(name = "level", nullable = false, length = 10)
    private String level;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    // --- Getters & Setters ---
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
