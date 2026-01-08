package com.example.logproducer.model;

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
    private String id;
    private String level;
    private String message;
    private String service;
    private String host;
    private Instant timestamp;
    private String traceId;
    private String userId;
    
    // Metadata for tracking
    private String environment;
    private String version;
}
