package com.example.logprocessor.consumer.model;

import javax.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the log_events table.
 *
 * The eventId column has a UNIQUE constraint — this is the database-level
 * safety net in case the bloom filter lets a duplicate through (false negative).
 * The insert uses MERGE / ON CONFLICT DO NOTHING semantics for idempotency.
 */
@Entity
@Table(name = "log_events", indexes = {
        @Index(name = "idx_log_events_timestamp", columnList = "event_timestamp"),
        @Index(name = "idx_log_events_service", columnList = "service_name"),
        @Index(name = "idx_log_events_correlation", columnList = "correlation_id")
})
public class PersistedLogEvent {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "source_region", length = 20, nullable = false)
    private String sourceRegion;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "level", length = 10)
    private String level;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "consuming_region", length = 20)
    private String consumingRegion;

    public PersistedLogEvent() {}

    public static PersistedLogEvent from(LogEvent event, String consumingRegion) {
        PersistedLogEvent entity = new PersistedLogEvent();
        entity.eventId = event.eventId();
        entity.sourceRegion = event.sourceRegion();
        entity.serviceName = event.serviceName();
        entity.level = event.level();
        entity.message = event.message();
        entity.eventTimestamp = event.eventTimestamp();
        entity.correlationId = event.correlationId();
        entity.consumedAt = Instant.now();
        entity.consumingRegion = consumingRegion;
        return entity;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getSourceRegion() { return sourceRegion; }
    public String getServiceName() { return serviceName; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public String getCorrelationId() { return correlationId; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getConsumingRegion() { return consumingRegion; }
}
