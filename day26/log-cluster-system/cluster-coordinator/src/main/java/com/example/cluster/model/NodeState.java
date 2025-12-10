package com.example.cluster.model;

import java.time.Instant;

public class NodeState {
    private String nodeId;
    private String ipAddress;
    private int port;
    private NodeStatus status;
    private int generation;
    private int healthScore;
    private Instant lastHeartbeatTime;
    private Instant lastStateChange;
    private String availabilityZone;
    private double phiScore;
    
    public enum NodeStatus {
        HEALTHY, SUSPECTED, FAILED, RECOVERING, LEAVING
    }
    
    public NodeState() {
        this.lastHeartbeatTime = Instant.now();
        this.lastStateChange = Instant.now();
        this.phiScore = 0.0;
    }
    
    public NodeState(String nodeId, String ipAddress, int port) {
        this();
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = NodeStatus.HEALTHY;
        this.generation = 1;
        this.healthScore = 100;
    }
    
    // Getters and setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { 
        this.status = status;
        this.lastStateChange = Instant.now();
    }
    
    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }
    
    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
    
    public Instant getLastHeartbeatTime() { return lastHeartbeatTime; }
    public void setLastHeartbeatTime(Instant lastHeartbeatTime) { 
        this.lastHeartbeatTime = lastHeartbeatTime; 
    }
    
    public Instant getLastStateChange() { return lastStateChange; }
    
    public String getAvailabilityZone() { return availabilityZone; }
    public void setAvailabilityZone(String availabilityZone) { 
        this.availabilityZone = availabilityZone; 
    }
    
    public double getPhiScore() { return phiScore; }
    public void setPhiScore(double phiScore) { this.phiScore = phiScore; }
    
    public void updateHeartbeat() {
        this.lastHeartbeatTime = Instant.now();
        this.phiScore = 0.0;  // Reset phi score on successful heartbeat
    }
    
    public long getMillisSinceLastHeartbeat() {
        return Instant.now().toEpochMilli() - lastHeartbeatTime.toEpochMilli();
    }
}
