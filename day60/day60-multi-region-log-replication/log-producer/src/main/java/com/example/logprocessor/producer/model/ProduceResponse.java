package com.example.logprocessor.producer.model;

/**
 * Outbound DTO confirming successful event production.
 * Returns the eventId so callers can use it for downstream correlation or deduplication checks.
 */
public record ProduceResponse(
        String eventId,
        String region,
        String topic,
        boolean success,
        String message
) {}
