package com.example.monitor.service;

import com.example.monitor.model.BottleneckReport;
import com.example.monitor.model.PerformanceMetrics;
import com.example.monitor.model.PerformanceReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates comprehensive performance analysis reports
 * Implements capacity planning pattern from AWS DynamoDB team
 */
@Slf4j
@Service
public class PerformanceReportGenerator {
    
    private final ClusterPerformanceMonitor performanceMonitor;
    private final BottleneckDetector bottleneckDetector;
    
    private final List<PerformanceMetrics> historicalMetrics = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 1000; // ~15 minutes at 1-second intervals
    
    public PerformanceReportGenerator(ClusterPerformanceMonitor performanceMonitor,
                                     BottleneckDetector bottleneckDetector) {
        this.performanceMonitor = performanceMonitor;
        this.bottleneckDetector = bottleneckDetector;
    }
    
    /**
     * Generate comprehensive performance report
     */
    public PerformanceReport generateReport(Instant periodStart, Instant periodEnd) {
        log.info("Generating performance report for period {} to {}", periodStart, periodEnd);
        
        var currentMetrics = performanceMonitor.getCurrentMetrics();
        var bottleneck = bottleneckDetector.detectBottleneck();
        
        // Calculate aggregate metrics
        double avgThroughput = calculateAverageThroughput(currentMetrics);
        double peakThroughput = calculatePeakThroughput(currentMetrics);
        
        var latencyPercentiles = performanceMonitor.getLatencyPercentiles();
        double avgLatencyP95 = latencyPercentiles.getOrDefault("p95", 0.0);
        
        // Capacity planning
        double currentCapacity = calculateCapacityUtilization(currentMetrics);
        double projectedCapacity = projectCapacityUtilization(currentCapacity, Duration.between(periodStart, periodEnd));
        Instant exhaustionDate = estimateCapacityExhaustion(currentCapacity, projectedCapacity);
        
        // Generate recommendations
        List<String> recommendations = generateOptimizationRecommendations(currentMetrics, bottleneck);
        Map<String, String> configTuning = generateConfigurationTuning(currentMetrics);
        
        return PerformanceReport.builder()
            .generatedAt(Instant.now())
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .avgThroughput(avgThroughput)
            .peakThroughput(peakThroughput)
            .avgLatencyP95(avgLatencyP95)
            .peakLatencyP95(avgLatencyP95 * 1.5) // Approximate peak
            .componentMetrics(currentMetrics)
            .bottlenecks(bottleneck.map(List::of).orElse(Collections.emptyList()))
            .currentCapacityUtilization(currentCapacity)
            .projectedCapacityUtilization(projectedCapacity)
            .capacityExhaustionDate(exhaustionDate)
            .optimizationRecommendations(recommendations)
            .configurationTuning(configTuning)
            .build();
    }
    
    private double calculateAverageThroughput(Map<String, PerformanceMetrics> metrics) {
        return metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getEventsPerSecond)
            .average()
            .orElse(0.0);
    }
    
    private double calculatePeakThroughput(Map<String, PerformanceMetrics> metrics) {
        return metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getEventsPerSecond)
            .max()
            .orElse(0.0);
    }
    
    /**
     * Calculate current capacity utilization (0.0 to 1.0)
     * Based on N+2 resilience model from Netflix
     */
    private double calculateCapacityUtilization(Map<String, PerformanceMetrics> metrics) {
        double avgCpu = metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getCpuUsagePercent)
            .average()
            .orElse(0.0) / 100.0;
            
        double avgMemory = metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getMemoryUsagePercent)
            .average()
            .orElse(0.0) / 100.0;
            
        return Math.max(avgCpu, avgMemory);
    }
    
    /**
     * Project capacity utilization based on growth trend
     */
    private double projectCapacityUtilization(double current, Duration period) {
        // Assume 10% growth per month (configurable based on traffic patterns)
        double monthlyGrowthRate = 0.10;
        double months = period.toDays() / 30.0;
        
        return current * Math.pow(1 + monthlyGrowthRate, months);
    }
    
    /**
     * Estimate when capacity will be exhausted (80% threshold)
     */
    private Instant estimateCapacityExhaustion(double current, double projected) {
        if (projected <= current || current >= 0.8) {
            return Instant.now().plus(Duration.ofDays(30)); // Conservative estimate
        }
        
        double monthlyGrowthRate = 0.10;
        double monthsToExhaustion = Math.log(0.8 / current) / Math.log(1 + monthlyGrowthRate);
        
        return Instant.now().plus(Duration.ofDays((long)(monthsToExhaustion * 30)));
    }
    
    /**
     * Generate actionable optimization recommendations
     */
    private List<String> generateOptimizationRecommendations(
            Map<String, PerformanceMetrics> metrics,
            Optional<BottleneckReport> bottleneck) {
        
        List<String> recommendations = new ArrayList<>();
        
        // Check for high latency
        var latencies = performanceMonitor.getLatencyPercentiles();
        double p95 = latencies.getOrDefault("p95", 0.0);
        if (p95 > 100.0) {
            recommendations.add("P95 latency exceeds 100ms - investigate slow components");
        }
        
        // Check for low throughput
        double avgThroughput = calculateAverageThroughput(metrics);
        if (avgThroughput < 3000.0) {
            recommendations.add("Throughput below target (3000 events/sec) - scale horizontally");
        }
        
        // Add bottleneck-specific recommendations
        bottleneck.ifPresent(b -> recommendations.addAll(b.getRecommendations()));
        
        // General optimization recommendations
        recommendations.add("Enable connection pooling with 2x concurrent request capacity");
        recommendations.add("Implement Redis cache warming during deployments");
        recommendations.add("Configure Kafka with partitions = 2x consumer count");
        
        return recommendations;
    }
    
    /**
     * Generate configuration tuning suggestions
     */
    private Map<String, String> generateConfigurationTuning(Map<String, PerformanceMetrics> metrics) {
        Map<String, String> tuning = new HashMap<>();
        
        double avgCpu = metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getCpuUsagePercent)
            .average()
            .orElse(0.0);
            
        if (avgCpu > 70.0) {
            tuning.put("jvm.threads.max", "Increase from 200 to 300");
            tuning.put("kafka.consumer.threads", "Match partition count");
        }
        
        double avgMemory = metrics.values().stream()
            .mapToDouble(PerformanceMetrics::getMemoryUsagePercent)
            .average()
            .orElse(0.0);
            
        if (avgMemory > 80.0) {
            tuning.put("jvm.heap.max", "Increase by 25%");
            tuning.put("jvm.gc.algorithm", "Consider G1GC for large heaps");
        }
        
        tuning.put("redis.connection.pool.size", "Set to 2x concurrent requests");
        tuning.put("postgresql.connection.pool.max", "Set to 50");
        tuning.put("kafka.batch.size", "16384 bytes for optimal throughput");
        
        return tuning;
    }
    
    /**
     * Record metrics for historical analysis
     */
    public void recordMetrics(PerformanceMetrics metrics) {
        historicalMetrics.add(metrics);
        
        // Trim history to prevent memory growth
        if (historicalMetrics.size() > MAX_HISTORY_SIZE) {
            historicalMetrics.remove(0);
        }
    }
}
