package com.example.anomalydetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class ClusteringDetector {
    private static final Logger logger = LoggerFactory.getLogger(ClusteringDetector.class);
    private static final double ANOMALY_THRESHOLD = 0.6;
    private final Random random = new Random(42);
    
    public List<AnomalyResult.DetectionInfo> detectAnomalies(LogEvent event) {
        List<AnomalyResult.DetectionInfo> detections = new ArrayList<>();
        
        // Extract feature vector
        double[] features = extractFeatures(event);
        
        // Compute isolation score (simplified isolation forest)
        double isolationScore = computeIsolationScore(features);
        
        if (isolationScore > ANOMALY_THRESHOLD) {
            detections.add(new AnomalyResult.DetectionInfo(
                "clustering", "multi-dimensional", isolationScore, ANOMALY_THRESHOLD));
            logger.info("Clustering anomaly detected: service={}, score={}", 
                       event.getServiceName(), isolationScore);
        }
        
        return detections;
    }
    
    private double[] extractFeatures(LogEvent event) {
        // Normalize features to 0-1 range
        return new double[] {
            event.getResponseTime() / 2000.0,
            event.getCpuUsage() / 100.0,
            event.getMemoryUsage() / 100.0,
            event.getErrorCount() / 30.0,
            event.getStatusCode() == 500 ? 1.0 : 0.0
        };
    }
    
    private double computeIsolationScore(double[] features) {
        // Simplified isolation forest: compute distance from expected center
        double[] expectedCenter = {0.05, 0.3, 0.5, 0.0, 0.0}; // Normal behavior
        
        double distance = 0.0;
        for (int i = 0; i < features.length; i++) {
            double diff = features[i] - expectedCenter[i];
            distance += diff * diff;
        }
        distance = Math.sqrt(distance);
        
        // Convert distance to anomaly score (0-1)
        return Math.min(1.0, distance);
    }
}
