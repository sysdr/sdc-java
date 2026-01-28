package com.example.logproducer;

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
    private Instant timestamp;
    private String level;           // ERROR, WARN, INFO, DEBUG
    private String service;         // auth-service, api-service, payment-service
    private String environment;     // prod, staging, dev
    private String host;            // prod-01, prod-02, staging-01
    private String region;          // us-east-1, us-west-2, eu-west-1
    private Integer statusCode;     // HTTP status codes
    private String errorType;       // NullPointerException, TimeoutException, etc.
    private String message;
    private Long durationMs;
    private String userId;
    private String traceId;
}
