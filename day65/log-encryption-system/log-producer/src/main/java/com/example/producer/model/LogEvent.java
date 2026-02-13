package com.example.producer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String severity;
    private Map<String, String> publicFields;  // Non-encrypted fields
    private Map<String, String> piiFields;     // Fields requiring encryption
    private Map<String, String> metadata;
}
