package com.example.loadgen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Results from load test execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestResult {
    private String testName;
    private Instant startTime;
    private Instant endTime;
    
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    
    private double avgThroughput;
    private double peakThroughput;
    
    private double avgLatencyMs;
    private double p50LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double maxLatencyMs;
    
    private Map<Integer, Long> statusCodeDistribution;
    private boolean testPassed;
    private String failureReason;
}
