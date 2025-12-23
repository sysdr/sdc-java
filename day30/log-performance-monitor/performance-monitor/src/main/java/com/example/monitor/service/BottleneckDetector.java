package com.example.monitor.service;

import com.example.monitor.model.BottleneckReport;
import com.example.monitor.model.PerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Automated bottleneck detection using resource utilization analysis
 * Implements comparative analysis pattern from Spotify's pipeline optimization
 */
@Slf4j
@Service
public class BottleneckDetector {
    
    private static final double CPU_THRESHOLD = 0.75;
    private static final double MEMORY_THRESHOLD = 0.80;
    private static final double THROUGHPUT_TARGET = 5000.0; // events/sec
    
    private final ClusterPerformanceMonitor performanceMonitor;
    private final Map<String, PerformanceMetrics> baselineMetrics = new HashMap<>();
    
    public BottleneckDetector(ClusterPerformanceMonitor performanceMonitor) {
        this.performanceMonitor = performanceMonitor;
    }
    
    /**
     * Detect bottlenecks using utilization vs throughput analysis
     */
    public Optional<BottleneckReport> detectBottleneck() {
        var currentMetrics = performanceMonitor.getCurrentMetrics();
        
        if (currentMetrics.isEmpty()) {
            return Optional.empty();
        }
        
        // Find component with highest utilization relative to throughput
        var bottleneck = currentMetrics.values().stream()
            .filter(m -> m.getCpuUsagePercent() > 0 || m.getMemoryUsagePercent() > 0)
            .max(Comparator.comparingDouble(this::calculateBottleneckScore))
            .orElse(null);
            
        if (bottleneck == null) {
            return Optional.empty();
        }
        
        // Check if this represents an actual bottleneck
        double score = calculateBottleneckScore(bottleneck);
        if (score < 0.5) { // Threshold for bottleneck severity
            return Optional.empty();
        }
        
        // Determine bottleneck type
        String bottleneckType = determineBottleneckType(bottleneck);
        
        // Generate recommendations
        List<String> recommendations = generateRecommendations(bottleneck, bottleneckType);
        String scalingStrategy = recommendScalingStrategy(bottleneck, bottleneckType);
        
        var report = BottleneckReport.builder()
            .detectedAt(Instant.now())
            .bottleneckComponent(bottleneck.getComponentName())
            .bottleneckType(bottleneckType)
            .severity(score)
            .currentMetrics(bottleneck)
            .baselineMetrics(baselineMetrics.get(bottleneck.getComponentName()))
            .recommendations(recommendations)
            .scalingStrategy(scalingStrategy)
            .build();
            
        log.warn("BOTTLENECK DETECTED: {} ({}), Severity: {}", 
            bottleneck.getComponentName(), bottleneckType, score);
            
        return Optional.of(report);
    }
    
    /**
     * Calculate bottleneck score: utilization vs throughput ratio
     * High utilization with low throughput indicates bottleneck
     */
    private double calculateBottleneckScore(PerformanceMetrics metrics) {
        double utilization = Math.max(
            metrics.getCpuUsagePercent() / 100.0,
            metrics.getMemoryUsagePercent() / 100.0
        );
        
        double throughputRatio = metrics.getEventsPerSecond() / THROUGHPUT_TARGET;
        
        // Score increases when utilization is high but throughput is low
        if (throughputRatio < 0.8 && utilization > 0.7) {
            return utilization / Math.max(throughputRatio, 0.1);
        }
        
        return 0.0;
    }
    
    /**
     * Determine bottleneck type from resource metrics
     */
    private String determineBottleneckType(PerformanceMetrics metrics) {
        if (metrics.getCpuUsagePercent() > CPU_THRESHOLD * 100) {
            return "CPU";
        }
        if (metrics.getMemoryUsagePercent() > MEMORY_THRESHOLD * 100) {
            return "MEMORY";
        }
        if (metrics.getQueueDepth() > 1000) {
            return "QUEUE_DEPTH";
        }
        if (metrics.getDiskIOUtilization() > 0.8) {
            return "DISK_IO";
        }
        if (metrics.getNetworkIOUtilization() > 0.8) {
            return "NETWORK_IO";
        }
        return "UNKNOWN";
    }
    
    /**
     * Generate optimization recommendations
     */
    private List<String> generateRecommendations(PerformanceMetrics metrics, String type) {
        List<String> recommendations = new ArrayList<>();
        
        switch (type) {
            case "CPU":
                recommendations.add("Consider horizontal scaling to distribute CPU load");
                recommendations.add("Profile application to identify CPU-intensive operations");
                recommendations.add("Enable CPU affinity for consistent performance");
                break;
            case "MEMORY":
                recommendations.add("Analyze heap dump for memory leaks");
                recommendations.add("Tune JVM heap size and GC algorithm");
                recommendations.add("Implement object pooling for frequently allocated objects");
                break;
            case "QUEUE_DEPTH":
                recommendations.add("Increase Kafka partition count for parallel processing");
                recommendations.add("Scale consumer instances to match partition count");
                recommendations.add("Implement backpressure mechanisms");
                break;
            case "DISK_IO":
                recommendations.add("Migrate to SSD storage for lower latency");
                recommendations.add("Implement write buffering and batch commits");
                recommendations.add("Consider in-memory caching with Redis");
                break;
            case "NETWORK_IO":
                recommendations.add("Increase network buffer sizes");
                recommendations.add("Enable connection pooling and reuse");
                recommendations.add("Consider data compression for network transfers");
                break;
        }
        
        return recommendations;
    }
    
    /**
     * Recommend scaling strategy based on bottleneck type
     */
    private String recommendScalingStrategy(PerformanceMetrics metrics, String type) {
        return switch (type) {
            case "CPU" -> "HORIZONTAL: Add 2-4 nodes to distribute CPU load";
            case "MEMORY" -> "VERTICAL: Increase memory by 50% per node";
            case "QUEUE_DEPTH" -> "HORIZONTAL: Add consumers matching partition count";
            case "DISK_IO" -> "INFRASTRUCTURE: Migrate to SSD/NVMe storage";
            case "NETWORK_IO" -> "INFRASTRUCTURE: Upgrade network bandwidth";
            default -> "ANALYSIS_REQUIRED: Manual investigation needed";
        };
    }
    
    /**
     * Store baseline metrics for comparison
     */
    public void captureBaseline() {
        var metrics = performanceMonitor.getCurrentMetrics();
        baselineMetrics.clear();
        baselineMetrics.putAll(metrics);
        log.info("Captured performance baseline for {} components", metrics.size());
    }
}
