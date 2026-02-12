package com.example.chaos.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExperimentResult {
    private String experimentName;
    private boolean success;
    private Instant startTime;
    private Instant endTime;
    private List<String> observations = new ArrayList<>();
    private String failureReason;
    
    // Getters and setters
    public String getExperimentName() { return experimentName; }
    public void setExperimentName(String experimentName) { this.experimentName = experimentName; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public List<String> getObservations() { return observations; }
    public void addObservation(String observation) { this.observations.add(observation); }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
