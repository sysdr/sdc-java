package com.example.chaos.model;

public class SteadyStateHypothesis {
    private String title;
    private double maxLatencyP95Ms;
    private double minSuccessRate;
    private int maxRecoveryTimeSeconds;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public double getMaxLatencyP95Ms() { return maxLatencyP95Ms; }
    public void setMaxLatencyP95Ms(double maxLatencyP95Ms) { this.maxLatencyP95Ms = maxLatencyP95Ms; }
    
    public double getMinSuccessRate() { return minSuccessRate; }
    public void setMinSuccessRate(double minSuccessRate) { this.minSuccessRate = minSuccessRate; }
    
    public int getMaxRecoveryTimeSeconds() { return maxRecoveryTimeSeconds; }
    public void setMaxRecoveryTimeSeconds(int maxRecoveryTimeSeconds) { 
        this.maxRecoveryTimeSeconds = maxRecoveryTimeSeconds; 
    }
}
