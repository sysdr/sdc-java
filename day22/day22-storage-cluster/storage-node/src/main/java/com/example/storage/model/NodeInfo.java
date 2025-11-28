package com.example.storage.model;

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
    private boolean isLeader;
    private int generationId;
    private Instant lastHeartbeat;
    private NodeStatus status;
    
    public enum NodeStatus {
        HEALTHY, DEGRADED, DOWN
    }
}
