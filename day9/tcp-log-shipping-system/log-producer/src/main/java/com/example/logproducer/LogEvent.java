package com.example.logproducer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String id;
    private String level;
    private String message;
    private String service;
    private Instant timestamp;
    private String traceId;
}
