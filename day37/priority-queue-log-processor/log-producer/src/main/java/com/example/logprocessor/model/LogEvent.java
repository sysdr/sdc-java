package com.example.logprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String id;
    private String message;
    private String level;
    private String service;
    private Instant timestamp;
    private Integer httpStatus;
    private Long latencyMs;
    private String exception;
    private String stackTrace;
    private PriorityLevel priority;
    
    public boolean containsException() {
        return exception != null && !exception.isEmpty();
    }
    
    public static LogEvent generateRandom() {
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        String[] services = {"api-gateway", "user-service", "payment-service", "notification-service"};
        String[] messages = {
            "Request processed successfully",
            "Database query slow",
            "Cache miss occurred",
            "API rate limit exceeded",
            "Connection timeout",
            "OutOfMemoryError in JVM",
            "Null pointer exception",
            "Payment processing failed",
            "User authentication failed",
            "Service mesh connection refused"
        };
        
        String message = messages[(int) (Math.random() * messages.length)];
        String level = levels[(int) (Math.random() * levels.length)];
        Integer httpStatus = Math.random() > 0.5 ? (int) (Math.random() * 500) + 100 : null;
        Long latency = (long) (Math.random() * 5000);
        
        String exception = null;
        String stackTrace = null;
        if (message.contains("Error") || message.contains("exception")) {
            exception = "java.lang." + (message.contains("OutOfMemory") ? "OutOfMemoryError" : "RuntimeException");
            stackTrace = "at com.example.service.Method.execute(Method.java:42)";
        }
        
        return LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .message(message)
                .level(level)
                .service(services[(int) (Math.random() * services.length)])
                .timestamp(Instant.now())
                .httpStatus(httpStatus)
                .latencyMs(latency)
                .exception(exception)
                .stackTrace(stackTrace)
                .build();
    }
}
