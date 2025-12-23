package com.example.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Automated bottleneck detection report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BottleneckReport {
    private Instant detectedAt;
    private String bottleneckComponent;
    private String bottleneckType; // CPU, MEMORY, DISK_IO, NETWORK_IO, QUEUE_DEPTH
    private double severity; // 0.0 to 1.0
    
    private PerformanceMetrics currentMetrics;
    private PerformanceMetrics baselineMetrics;
    
    private List<String> recommendations;
    private String scalingStrategy;
}
