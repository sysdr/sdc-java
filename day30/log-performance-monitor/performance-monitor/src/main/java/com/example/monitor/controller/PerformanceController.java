package com.example.monitor.controller;

import com.example.monitor.model.BottleneckReport;
import com.example.monitor.model.PerformanceMetrics;
import com.example.monitor.model.PerformanceReport;
import com.example.monitor.service.BottleneckDetector;
import com.example.monitor.service.ClusterPerformanceMonitor;
import com.example.monitor.service.PerformanceReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for performance monitoring and reporting
 */
@Slf4j
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    
    private final ClusterPerformanceMonitor performanceMonitor;
    private final BottleneckDetector bottleneckDetector;
    private final PerformanceReportGenerator reportGenerator;
    
    public PerformanceController(ClusterPerformanceMonitor performanceMonitor,
                                BottleneckDetector bottleneckDetector,
                                PerformanceReportGenerator reportGenerator) {
        this.performanceMonitor = performanceMonitor;
        this.bottleneckDetector = bottleneckDetector;
        this.reportGenerator = reportGenerator;
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, PerformanceMetrics>> getCurrentMetrics() {
        return ResponseEntity.ok(performanceMonitor.getCurrentMetrics());
    }
    
    @GetMapping("/latency")
    public ResponseEntity<Map<String, Double>> getLatencyPercentiles() {
        return ResponseEntity.ok(performanceMonitor.getLatencyPercentiles());
    }
    
    @GetMapping("/bottlenecks")
    public ResponseEntity<Optional<BottleneckReport>> detectBottlenecks() {
        return ResponseEntity.ok(bottleneckDetector.detectBottleneck());
    }
    
    @PostMapping("/baseline")
    public ResponseEntity<String> captureBaseline() {
        bottleneckDetector.captureBaseline();
        return ResponseEntity.ok("Baseline captured successfully");
    }
    
    @GetMapping("/report")
    public ResponseEntity<PerformanceReport> generateReport(
            @RequestParam(required = false) Long durationMinutes) {
        
        Instant end = Instant.now();
        Instant start = end.minus(durationMinutes != null ? durationMinutes : 60, ChronoUnit.MINUTES);
        
        PerformanceReport report = reportGenerator.generateReport(start, end);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/record")
    public ResponseEntity<String> recordEvent(
            @RequestParam String component,
            @RequestParam long latencyMs) {
        
        performanceMonitor.recordEvent(component, latencyMs);
        return ResponseEntity.ok("Event recorded");
    }
}
