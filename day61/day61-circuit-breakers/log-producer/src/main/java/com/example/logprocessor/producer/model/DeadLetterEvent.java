package com.example.logprocessor.producer.model;

import java.time.Instant;

/**
 * Wraps events that failed to reach Kafka.
 * In production these would be persisted to a durable store for reconciliation.
 */
public record DeadLetterEvent(
        LogEvent originalEvent,
        String failureReason,
        int attemptCount,
        Instant failedAt
) {}
