package com.example.logproducer.model;

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
    private String level;
    private String message;
    private String source;
    private String userId;
    private Instant timestamp;
    private boolean simulateFailure;
}
