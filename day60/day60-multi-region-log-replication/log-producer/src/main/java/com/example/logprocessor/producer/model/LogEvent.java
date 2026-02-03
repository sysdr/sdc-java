package com.example.logprocessor.producer.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value object representing a single log event.
 *
 * Key fields for multi-region replication:
 *   - eventId:     Globally unique identifier used for deduplication across regions.
 *   - eventTimestamp: Producer-side wall-clock time; used by the consumer reorder buffer
 *                     to reconstruct causal ordering after cross-region replication.
 *   - sourceRegion: The region where this event was originally produced. Consumers
 *                   use this to detect duplicates that originated in a different region.
 */
public record LogEvent(
        String eventId,
        String sourceRegion,
        String serviceName,
        String level,           // DEBUG | INFO | WARN | ERROR | FATAL
        String message,
        Instant eventTimestamp,
        String correlationId    // Distributed tracing correlation
) {
    /** Factory method: generates a new LogEvent with a fresh UUID and current timestamp. */
    public static LogEvent create(String sourceRegion, String serviceName, String level, String message, String correlationId) {
        return new LogEvent(
                UUID.randomUUID().toString(),
                sourceRegion,
                serviceName,
                level,
                message,
                Instant.now(),
                correlationId
        );
    }
}
