package com.example.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("timestamp")
    private long timestamp; // Event time in millis
    
    @JsonProperty("service")
    private String service;
    
    @JsonProperty("level")
    private String level; // INFO, WARN, ERROR
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("latency_ms")
    private Integer latencyMs;
    
    @JsonProperty("status_code")
    private Integer statusCode;
    
    @JsonProperty("attributes")
    private Map<String, String> attributes;
}
