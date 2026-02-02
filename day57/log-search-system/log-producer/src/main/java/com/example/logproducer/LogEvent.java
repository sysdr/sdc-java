package com.example.logproducer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("severity")
    private String severity; // ERROR, WARN, INFO, DEBUG
    
    @JsonProperty("service_name")
    private String serviceName;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("stack_trace")
    private String stackTrace;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("trace_id")
    private String traceId;
    
    @JsonProperty("span_id")
    private String spanId;
    
    public static LogEvent createSample(String serviceName, String severity, String message) {
        return LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .severity(severity)
                .serviceName(serviceName)
                .message(message)
                .traceId(UUID.randomUUID().toString())
                .spanId(UUID.randomUUID().toString().substring(0, 16))
                .build();
    }
}
