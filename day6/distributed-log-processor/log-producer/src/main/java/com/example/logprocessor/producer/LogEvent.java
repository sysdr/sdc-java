package com.example.logprocessor.producer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.Map;

public record LogEvent(
    @NotBlank(message = "Message cannot be blank")
    String message,
    
    @NotNull(message = "Log level is required")
    @Pattern(regexp = "DEBUG|INFO|WARN|ERROR|FATAL", message = "Invalid log level")
    String level,
    
    @NotBlank(message = "Source cannot be blank")
    String source,
    
    Instant timestamp,
    
    Map<String, Object> metadata
) {
    public LogEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
