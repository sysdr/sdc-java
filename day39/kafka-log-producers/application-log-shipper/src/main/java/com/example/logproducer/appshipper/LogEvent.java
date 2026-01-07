package com.example.logproducer.appshipper;

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
    private String eventId;
    private String source;
    private String level;
    private String message;
    private Instant timestamp;
    private String serviceId;
    private String traceId;
    private Map<String, String> metadata;
}
