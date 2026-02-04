package com.example.logprocessor.producer.model;

import java.time.Instant;

/**
 * Canonical log event model used throughout the producer.
 * This is what gets serialized to Kafka.
 */
public record LogEvent(
        String eventId,
        String source,
        String level,
        String message,
        Instant timestamp,
        String correlationId
) {}
