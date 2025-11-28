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
public class NodeMetadata {
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
    
    public String getUrl() {
        return "http://" + host + ":" + port;
    }
}
