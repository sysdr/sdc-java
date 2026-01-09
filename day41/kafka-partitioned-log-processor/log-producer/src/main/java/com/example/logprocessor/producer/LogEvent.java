package com.example.logprocessor.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String eventId;
    private String source;           // Partition key
    private String level;            // INFO, WARN, ERROR
    private String message;
    private String application;
    private String hostname;
    private Instant timestamp;
    private String traceId;
}
