package com.example.logprocessor.gateway.model;

import jakarta.validation.constraints.*;
import java.time.Instant;

/**
 * Inbound log event payload. Validated at the gateway boundary before
 * anything touches Kafka or any downstream service.
 */
public record LogEventRequest(
        @NotBlank(message = "source is required")
        @Size(max = 128)
        String source,

        @NotNull(message = "level is required")
        LogLevel level,

        @NotBlank(message = "message is required")
        @Size(max = 8192)
        String message,

        // If the client doesn't supply a timestamp, the gateway stamps it.
        Instant timestamp
) {
    public LogEventRequest {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR, FATAL
    }
}
