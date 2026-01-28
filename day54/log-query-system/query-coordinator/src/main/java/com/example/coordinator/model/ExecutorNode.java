package com.example.coordinator.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ExecutorNode {
    private String nodeId;
    private String host;
    private int port;
    private boolean healthy;
    private Instant lastHealthCheck;
    private int partitionId;
    private long totalLogs;
}
