package com.example.logprocessor.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfo {
    private String nodeId;
    private String host;
    private int port;
    private Instant lastHeartbeat;
    private String status; // ACTIVE, SUSPECTED, FAILED
    private long logCount;
    private double loadPercentage;
}
