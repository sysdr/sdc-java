package com.example.logprocessor.consumer.model;

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
    private String id;
    private String severity;
    private String category;
    private String message;
    private String source;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
