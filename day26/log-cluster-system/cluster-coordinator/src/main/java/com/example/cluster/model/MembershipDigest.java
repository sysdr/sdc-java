package com.example.cluster.model;

import java.util.HashMap;
import java.util.Map;

public class MembershipDigest {
    private String sourceNodeId;
    private int sourceGeneration;
    private Map<String, NodeState> members;
    private long timestamp;
    
    public MembershipDigest() {
        this.members = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public MembershipDigest(String sourceNodeId, int sourceGeneration) {
        this();
        this.sourceNodeId = sourceNodeId;
        this.sourceGeneration = sourceGeneration;
    }
    
    public void addMember(String nodeId, NodeState state) {
        members.put(nodeId, state);
    }
    
    // Getters and setters
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    
    public int getSourceGeneration() { return sourceGeneration; }
    public void setSourceGeneration(int sourceGeneration) { 
        this.sourceGeneration = sourceGeneration; 
    }
    
    public Map<String, NodeState> getMembers() { return members; }
    public void setMembers(Map<String, NodeState> members) { this.members = members; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
