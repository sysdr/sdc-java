package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private String id;
    private Instant timestamp;
    private String level;
    private String serviceName;
    private String message;
    private String traceId;
    private String partitionId;
}
