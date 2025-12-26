package com.systemdesign.logprocessor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String applicationName;
    private String level;
    private String message;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    
    private String host;
    private String service;
    private Map<String, String> metadata;
    private String traceId;
    private Long processingTimestamp;
    private String enrichedData;
}
