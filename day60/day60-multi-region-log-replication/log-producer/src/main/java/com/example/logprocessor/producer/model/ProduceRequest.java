package com.example.logprocessor.producer.model;

/**
 * Inbound DTO for the /logs endpoint.
 * Decoupled from LogEvent so the API contract can evolve independently.
 */
public record ProduceRequest(
        String serviceName,
        String level,
        String message,
        String correlationId
) {}
