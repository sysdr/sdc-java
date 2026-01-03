package com.example.logproducer;

public class BatchRequest {
    private int count;
    private int failureRate; // 0-100
    
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    public int getFailureRate() { return failureRate; }
    public void setFailureRate(int failureRate) { this.failureRate = failureRate; }
}
