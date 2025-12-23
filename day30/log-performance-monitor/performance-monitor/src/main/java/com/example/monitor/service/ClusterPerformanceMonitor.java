package com.example.monitor.service;

import com.example.monitor.model.PerformanceMetrics;
import com.example.monitor.model.BottleneckReport;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and analyzes cluster-wide performance metrics
 * Implements multi-dimensional metric collection pattern
 */
@Slf4j
@Service
public class ClusterPerformanceMonitor {
    
    private final MeterRegistry registry;
    private final Map<String, PerformanceMetrics> componentMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    
    // Metric instruments
    private final Counter totalEventsProcessed;
    private final DistributionSummary latencyDistribution;
    private final Gauge cpuGauge;
    private final Gauge memoryGauge;
    
    // JMX beans for system metrics
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    public ClusterPerformanceMonitor(MeterRegistry registry) {
        this.registry = registry;
        
        // Initialize metric instruments with percentile tracking
        this.totalEventsProcessed = Counter.builder("cluster.events.total")
            .description("Total events processed across cluster")
            .register(registry);
            
        this.latencyDistribution = DistributionSummary.builder("cluster.latency")
            .description("End-to-end processing latency")
            .baseUnit("milliseconds")
            .publishPercentiles(0.5, 0.95, 0.99, 0.999)
            .publishPercentileHistogram()
            .register(registry);
            
        this.cpuGauge = Gauge.builder("system.cpu.usage", this, ClusterPerformanceMonitor::getCpuUsage)
            .description("System CPU usage percentage")
            .register(registry);
            
        this.memoryGauge = Gauge.builder("system.memory.usage", this, ClusterPerformanceMonitor::getMemoryUsage)
            .description("System memory usage percentage")
            .register(registry);
        
        // Initialize JMX beans
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        log.info("Cluster Performance Monitor initialized with percentile tracking");
    }
    
    /**
     * Record event processing with latency tracking
     */
    public void recordEvent(String component, long latencyMs) {
        totalEventsProcessed.increment();
        latencyDistribution.record(latencyMs);
        
        eventCounters.computeIfAbsent(component, k -> new AtomicLong()).incrementAndGet();
    }
    
    /**
     * Collect system-level metrics every second
     * Trade-off: High-resolution data vs storage overhead
     */
    @Scheduled(fixedRate = 1000)
    public void collectSystemMetrics() {
        try {
            var metrics = PerformanceMetrics.builder()
                .componentName("system")
                .timestamp(Instant.now())
                .cpuUsagePercent(getCpuUsage())
                .memoryUsagePercent(getMemoryUsage())
                .build();
                
            componentMetrics.put("system", metrics);
            
            // Log warning if resource usage is critical
            if (metrics.getCpuUsagePercent() > 80.0) {
                log.warn("HIGH CPU USAGE: {}%", metrics.getCpuUsagePercent());
            }
            if (metrics.getMemoryUsagePercent() > 85.0) {
                log.warn("HIGH MEMORY USAGE: {}%", metrics.getMemoryUsagePercent());
            }
            
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
        }
    }
    
    /**
     * Aggregate component metrics every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void aggregateComponentMetrics() {
        eventCounters.forEach((component, counter) -> {
            long count = counter.getAndSet(0);
            double eventsPerSecond = count / 5.0; // 5-second window
            
            var metrics = PerformanceMetrics.builder()
                .componentName(component)
                .timestamp(Instant.now())
                .eventsProcessed(count)
                .eventsPerSecond(eventsPerSecond)
                .build();
                
            componentMetrics.put(component, metrics);
        });
    }
    
    /**
     * Get current CPU usage percentage
     */
    private double getCpuUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double cpuLoad = sunOsBean.getProcessCpuLoad();
            return cpuLoad >= 0 ? cpuLoad * 100.0 : 0.0;
        }
        return 0.0;
    }
    
    /**
     * Get current memory usage percentage
     */
    private double getMemoryUsage() {
        var heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        return max > 0 ? (used * 100.0 / max) : 0.0;
    }
    
    /**
     * Get GC metrics for memory pressure analysis
     */
    public Map<String, Long> getGcMetrics() {
        Map<String, Long> gcMetrics = new HashMap<>();
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            gcMetrics.put(gcBean.getName() + ".count", gcBean.getCollectionCount());
            gcMetrics.put(gcBean.getName() + ".time", gcBean.getCollectionTime());
        }
        
        return gcMetrics;
    }
    
    /**
     * Get current performance snapshot for all components
     */
    public Map<String, PerformanceMetrics> getCurrentMetrics() {
        return new HashMap<>(componentMetrics);
    }
    
    /**
     * Get latency percentiles from distribution summary
     */
    public Map<String, Double> getLatencyPercentiles() {
        Map<String, Double> percentiles = new HashMap<>();
        
        Arrays.stream(latencyDistribution.takeSnapshot().percentileValues()).forEach(valueAtPercentile -> {
            String percentile = String.format("p%d", (int)(valueAtPercentile.percentile() * 100));
            percentiles.put(percentile, valueAtPercentile.value());
        });
        
        return percentiles;
    }
}
