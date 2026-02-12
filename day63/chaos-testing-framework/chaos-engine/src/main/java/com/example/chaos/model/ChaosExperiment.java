package com.example.chaos.model;

import java.time.Duration;
import java.util.List;

public class ChaosExperiment {
    private String name;
    private String description;
    private List<String> targets;
    private FailureType failureType;
    private Duration duration;
    private SteadyStateHypothesis hypothesis;
    
    public enum FailureType {
        SERVICE_KILL,
        NETWORK_LATENCY,
        RESOURCE_EXHAUSTION,
        PACKET_LOSS,
        DATABASE_UNAVAILABLE
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getTargets() { return targets; }
    public void setTargets(List<String> targets) { this.targets = targets; }
    
    public FailureType getFailureType() { return failureType; }
    public void setFailureType(FailureType failureType) { this.failureType = failureType; }
    
    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }
    
    public SteadyStateHypothesis getHypothesis() { return hypothesis; }
    public void setHypothesis(SteadyStateHypothesis hypothesis) { this.hypothesis = hypothesis; }
}
