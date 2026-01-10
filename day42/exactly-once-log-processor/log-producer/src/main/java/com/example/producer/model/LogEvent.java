package com.example.producer.model;

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
    private String eventType;
    private String service;
    private String message;
    private String severity;
    private Instant timestamp;
    private String userId;
    private String traceId;
}
