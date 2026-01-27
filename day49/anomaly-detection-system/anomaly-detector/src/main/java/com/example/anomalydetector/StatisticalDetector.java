package com.example.anomalydetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatisticalDetector {
    private static final Logger logger = LoggerFactory.getLogger(StatisticalDetector.class);
    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final int WINDOW_SIZE = 100;
    
    private final Map<String, MetricWindow> windows = new ConcurrentHashMap<>();
    
    public List<AnomalyResult.DetectionInfo> detectAnomalies(LogEvent event) {
        List<AnomalyResult.DetectionInfo> detections = new ArrayList<>();
        
        // Detect anomalies in response time
        String responseTimeKey = event.getServiceName() + ":responseTime";
        double responseTimeZScore = calculateZScore(responseTimeKey, event.getResponseTime());
        if (Math.abs(responseTimeZScore) > Z_SCORE_THRESHOLD) {
            detections.add(new AnomalyResult.DetectionInfo(
                "z-score", "responseTime", responseTimeZScore, Z_SCORE_THRESHOLD));
            logger.info("Response time anomaly detected: service={}, value={}, z-score={}", 
                       event.getServiceName(), event.getResponseTime(), responseTimeZScore);
        }
        
        // Detect anomalies in CPU usage
        String cpuKey = event.getServiceName() + ":cpuUsage";
        double cpuZScore = calculateZScore(cpuKey, event.getCpuUsage());
        if (Math.abs(cpuZScore) > Z_SCORE_THRESHOLD) {
            detections.add(new AnomalyResult.DetectionInfo(
                "z-score", "cpuUsage", cpuZScore, Z_SCORE_THRESHOLD));
            logger.info("CPU usage anomaly detected: service={}, value={}, z-score={}", 
                       event.getServiceName(), event.getCpuUsage(), cpuZScore);
        }
        
        // Detect anomalies in memory usage
        String memoryKey = event.getServiceName() + ":memoryUsage";
        double memoryZScore = calculateZScore(memoryKey, event.getMemoryUsage());
        if (Math.abs(memoryZScore) > Z_SCORE_THRESHOLD) {
            detections.add(new AnomalyResult.DetectionInfo(
                "z-score", "memoryUsage", memoryZScore, Z_SCORE_THRESHOLD));
            logger.info("Memory usage anomaly detected: service={}, value={}, z-score={}", 
                       event.getServiceName(), event.getMemoryUsage(), memoryZScore);
        }
        
        // Detect anomalies in error count
        String errorKey = event.getServiceName() + ":errorCount";
        double errorZScore = calculateZScore(errorKey, event.getErrorCount());
        if (Math.abs(errorZScore) > Z_SCORE_THRESHOLD) {
            detections.add(new AnomalyResult.DetectionInfo(
                "z-score", "errorCount", errorZScore, Z_SCORE_THRESHOLD));
            logger.info("Error count anomaly detected: service={}, value={}, z-score={}", 
                       event.getServiceName(), event.getErrorCount(), errorZScore);
        }
        
        return detections;
    }
    
    private double calculateZScore(String key, double value) {
        MetricWindow window = windows.computeIfAbsent(key, k -> new MetricWindow(WINDOW_SIZE));
        
        double mean = window.getMean();
        double stdDev = window.getStdDev();
        
        window.addValue(value);
        
        if (stdDev == 0) {
            return 0.0;
        }
        
        return (value - mean) / stdDev;
    }
    
    private static class MetricWindow {
        private final int maxSize;
        private final LinkedList<Double> values;
        private double sum;
        private double sumOfSquares;
        
        public MetricWindow(int maxSize) {
            this.maxSize = maxSize;
            this.values = new LinkedList<>();
            this.sum = 0.0;
            this.sumOfSquares = 0.0;
        }
        
        public void addValue(double value) {
            if (values.size() >= maxSize) {
                double removed = values.removeFirst();
                sum -= removed;
                sumOfSquares -= removed * removed;
            }
            
            values.addLast(value);
            sum += value;
            sumOfSquares += value * value;
        }
        
        public double getMean() {
            return values.isEmpty() ? 0.0 : sum / values.size();
        }
        
        public double getStdDev() {
            if (values.size() < 2) {
                return 0.0;
            }
            
            double mean = getMean();
            double variance = (sumOfSquares / values.size()) - (mean * mean);
            return Math.sqrt(Math.max(0, variance));
        }
    }
}
