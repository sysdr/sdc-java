package com.example.anomalydetector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyResult {
    private String eventId;
    private String serviceName;
    private long timestamp;
    private double confidence;
    private List<DetectionInfo> detections;
    private LogEvent originalEvent;
    
    public AnomalyResult(String eventId, String serviceName, long timestamp) {
        this.eventId = eventId;
        this.serviceName = serviceName;
        this.timestamp = timestamp;
        this.confidence = 0.0;
        this.detections = new ArrayList<>();
    }
    
    public void addDetection(String method, String metric, double score, double threshold) {
        this.detections.add(new DetectionInfo(method, metric, score, threshold));
        updateConfidence();
    }
    
    private void updateConfidence() {
        double zScoreWeight = 0.4;
        double timeSeriesWeight = 0.3;
        double clusteringWeight = 0.3;
        
        double totalConfidence = 0.0;
        int detectorCount = 0;
        
        for (DetectionInfo detection : detections) {
            detectorCount++;
            switch (detection.getMethod()) {
                case "z-score" -> totalConfidence += zScoreWeight;
                case "time-series" -> totalConfidence += timeSeriesWeight;
                case "clustering" -> totalConfidence += clusteringWeight;
            }
        }
        
        // Boost confidence if multiple detectors agree
        if (detectorCount > 1) {
            totalConfidence *= (1.0 + 0.2 * (detectorCount - 1));
        }
        
        this.confidence = Math.min(1.0, totalConfidence);
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionInfo {
        private String method;
        private String metric;
        private double score;
        private double threshold;
    }
}
