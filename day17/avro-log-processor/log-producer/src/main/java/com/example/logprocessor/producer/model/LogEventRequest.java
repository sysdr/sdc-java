package com.example.logprocessor.producer.model;

import java.util.Map;

public record LogEventRequest(
    String level,
    String message,
    String source,
    String correlationId,
    Map<String, String> tags,
    String spanId,
    String parentSpanId,
    Integer schemaVersion
) {
    public LogEventRequest {
        if (schemaVersion == null) {
            schemaVersion = 2;
        }
    }
}
