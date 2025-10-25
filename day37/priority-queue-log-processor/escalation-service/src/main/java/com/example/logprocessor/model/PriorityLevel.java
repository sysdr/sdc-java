package com.example.logprocessor.model;

public enum PriorityLevel {
    CRITICAL(1, 0),
    HIGH(2, 60000),      // Escalate after 60s
    NORMAL(3, 30000),    // Escalate after 30s
    LOW(4, 120000);      // Escalate after 120s
    
    private final int value;
    private final long escalationThresholdMs;
    
    PriorityLevel(int value, long escalationThresholdMs) {
        this.value = value;
        this.escalationThresholdMs = escalationThresholdMs;
    }
    
    public int getValue() {
        return value;
    }
    
    public long getEscalationThresholdMs() {
        return escalationThresholdMs;
    }
    
    public String getTopicName() {
        return this.name().toLowerCase() + "-logs";
    }
    
    public PriorityLevel escalate() {
        switch(this) {
            case LOW: return NORMAL;
            case NORMAL: return HIGH;
            case HIGH: return CRITICAL;
            default: return CRITICAL;
        }
    }
}
