package com.example.anomalydetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TimeSeriesDetector {
    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesDetector.class);
    private static final double DEVIATION_THRESHOLD = 0.5; // 50% deviation
    private static final double ALPHA = 0.3; // Smoothing factor
    
    private final Map<String, ExponentialSmoothing> models = new ConcurrentHashMap<>();
    
    public List<AnomalyResult.DetectionInfo> detectAnomalies(LogEvent event) {
        List<AnomalyResult.DetectionInfo> detections = new ArrayList<>();
        
        // Detect anomalies in response time
        String responseTimeKey = event.getServiceName() + ":responseTime";
        double responseTimeForecast = predict(responseTimeKey, event.getResponseTime());
        if (responseTimeForecast > 0) {
            double deviation = Math.abs(event.getResponseTime() - responseTimeForecast) / responseTimeForecast;
            if (deviation > DEVIATION_THRESHOLD) {
                detections.add(new AnomalyResult.DetectionInfo(
                    "time-series", "responseTime", deviation, DEVIATION_THRESHOLD));
                logger.info("Time-series anomaly in response time: service={}, actual={}, forecast={}, deviation={}", 
                           event.getServiceName(), event.getResponseTime(), responseTimeForecast, deviation);
            }
        }
        
        // Detect anomalies in CPU usage
        String cpuKey = event.getServiceName() + ":cpuUsage";
        double cpuForecast = predict(cpuKey, event.getCpuUsage());
        if (cpuForecast > 0) {
            double deviation = Math.abs(event.getCpuUsage() - cpuForecast) / cpuForecast;
            if (deviation > DEVIATION_THRESHOLD) {
                detections.add(new AnomalyResult.DetectionInfo(
                    "time-series", "cpuUsage", deviation, DEVIATION_THRESHOLD));
                logger.info("Time-series anomaly in CPU usage: service={}, actual={}, forecast={}, deviation={}", 
                           event.getServiceName(), event.getCpuUsage(), cpuForecast, deviation);
            }
        }
        
        return detections;
    }
    
    private double predict(String key, double actualValue) {
        ExponentialSmoothing model = models.computeIfAbsent(key, k -> new ExponentialSmoothing(ALPHA));
        double forecast = model.getForecast();
        model.update(actualValue);
        return forecast;
    }
    
    private static class ExponentialSmoothing {
        private final double alpha;
        private double level;
        private boolean initialized;
        
        public ExponentialSmoothing(double alpha) {
            this.alpha = alpha;
            this.level = 0.0;
            this.initialized = false;
        }
        
        public void update(double value) {
            if (!initialized) {
                level = value;
                initialized = true;
            } else {
                level = alpha * value + (1 - alpha) * level;
            }
        }
        
        public double getForecast() {
            return initialized ? level : 0.0;
        }
    }
}
