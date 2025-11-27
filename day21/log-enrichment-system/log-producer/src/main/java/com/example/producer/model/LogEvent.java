package com.example.producer.model;

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
    private String id;
    private Instant timestamp;
    private String level;
    private String message;
    private String service;
    
    @JsonProperty("source_ip")
    private String sourceIp;
    
    @JsonProperty("log_schema_version")
    private String logSchemaVersion;
    
    private Map<String, Object> attributes;
}
