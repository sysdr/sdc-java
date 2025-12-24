package com.example.logproducer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    
    private String eventId;
    
    @NotBlank(message = "Source cannot be blank")
    private String source;
    
    @NotNull(message = "Level cannot be null")
    private LogLevel level;
    
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    private Instant timestamp;
    
    private String hostname;
    
    private String correlationId;
    
    private Map<String, String> metadata;
    
    private String stackTrace;
    
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
}
