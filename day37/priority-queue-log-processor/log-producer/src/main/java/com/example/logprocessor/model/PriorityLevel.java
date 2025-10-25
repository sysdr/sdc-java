package com.example.logprocessor.model;

public enum PriorityLevel {
    CRITICAL(1),
    HIGH(2),
    NORMAL(3),
    LOW(4);
    
    private final int value;
    
    PriorityLevel(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getTopicName() {
        return this.name().toLowerCase() + "-logs";
    }
}
