package com.example.logindexer;

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
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("severity")
    private String severity;
    
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
}
