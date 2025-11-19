package com.example.logprocessor.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalLog {
    private String id;
    private Instant timestamp;
    private LogLevel level;
    private String service;
    private String host;
    private String message;
    private String traceId;
    private String spanId;
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    private String rawContent;

    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL;

        public static LogLevel fromString(String level) {
            if (level == null) return INFO;
            try {
                return LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return INFO;
            }
        }
    }
}
