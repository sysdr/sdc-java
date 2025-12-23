package com.example.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive performance analysis report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReport {
    private Instant generatedAt;
    private Instant periodStart;
    private Instant periodEnd;
    
    // Aggregate metrics
    private double avgThroughput;
    private double peakThroughput;
    private double avgLatencyP95;
    private double peakLatencyP95;
    
    // Per-component metrics
    private Map<String, PerformanceMetrics> componentMetrics;
    
    // Bottleneck analysis
    private List<BottleneckReport> bottlenecks;
    
    // Capacity planning
    private double currentCapacityUtilization;
    private double projectedCapacityUtilization;
    private Instant capacityExhaustionDate;
    
    // Recommendations
    private List<String> optimizationRecommendations;
    private Map<String, String> configurationTuning;
}
