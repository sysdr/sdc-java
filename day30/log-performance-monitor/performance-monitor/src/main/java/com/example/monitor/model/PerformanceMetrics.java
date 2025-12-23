package com.example.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Performance metrics snapshot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private String componentName;
    private Instant timestamp;
    
    // Throughput metrics
    private long eventsProcessed;
    private double eventsPerSecond;
    
    // Latency metrics (milliseconds)
    private double latencyP50;
    private double latencyP95;
    private double latencyP99;
    private double latencyMax;
    
    // Resource utilization
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private double diskIOUtilization;
    private double networkIOUtilization;
    
    // Error metrics
    private long errorCount;
    private double errorRate;
    
    // Queue metrics
    private long queueDepth;
    private double queueWaitTime;
}
