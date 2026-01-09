package com.example.logprocessor.consumer;

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
    private String source;
    private String level;
    private String message;
    private String application;
    private String hostname;
    private Instant timestamp;
    private String traceId;
}
