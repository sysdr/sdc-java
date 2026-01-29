package com.example.logindexing.producer;

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
    private Instant timestamp;
    private String level;
    private String service;
    private String message;
    private String userId;
    private String traceId;
    private Map<String, String> metadata;
}
