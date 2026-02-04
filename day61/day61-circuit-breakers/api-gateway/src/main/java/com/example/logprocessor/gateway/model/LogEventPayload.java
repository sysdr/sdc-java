package com.example.logprocessor.gateway.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal payload forwarded to the log-producer service.
 * Includes a correlation ID for distributed tracing.
 */
public record LogEventPayload(
        String eventId,
        String source,
        String level,
        String message,
        Instant timestamp,
        String correlationId
) {
    /**
     * Factory: create from a validated request, attaching trace IDs.
     */
    public static LogEventPayload from(LogEventRequest req, String correlationId) {
        return new LogEventPayload(
                UUID.randomUUID().toString(),
                req.source(),
                req.level().name(),
                req.message(),
                req.timestamp(),
                correlationId
        );
    }
}
