package com.example.logprocessor.consumer.model;

import java.time.Instant;

/**
 * Consumer-side representation of a log event.
 * Mirrors the producer's record but is a separate class to decouple
 * the two modules' compile-time dependencies.
 */
public record LogEvent(
        String eventId,
        String sourceRegion,
        String serviceName,
        String level,
        String message,
        Instant eventTimestamp,
        String correlationId
) {}
