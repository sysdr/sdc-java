package com.example.anomalydetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private static final String ANOMALY_TOPIC = "detected-anomalies";
    
    private final StatisticalDetector statisticalDetector;
    private final TimeSeriesDetector timeSeriesDetector;
    private final ClusteringDetector clusteringDetector;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AnomalyRepository anomalyRepository;
    private final ObjectMapper objectMapper;
    
    public AnomalyDetectionService(
            StatisticalDetector statisticalDetector,
            TimeSeriesDetector timeSeriesDetector,
            ClusteringDetector clusteringDetector,
            KafkaTemplate<String, String> kafkaTemplate,
            RedisTemplate<String, Object> redisTemplate,
            AnomalyRepository anomalyRepository,
            ObjectMapper objectMapper) {
        this.statisticalDetector = statisticalDetector;
        this.timeSeriesDetector = timeSeriesDetector;
        this.clusteringDetector = clusteringDetector;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.anomalyRepository = anomalyRepository;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "log-events", groupId = "anomaly-detector")
    @CircuitBreaker(name = "anomaly-detection", fallbackMethod = "fallbackDetection")
    public void processLogEvent(String message) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            
            // Run all detectors
            List<AnomalyResult.DetectionInfo> allDetections = new ArrayList<>();
            allDetections.addAll(statisticalDetector.detectAnomalies(event));
            allDetections.addAll(timeSeriesDetector.detectAnomalies(event));
            allDetections.addAll(clusteringDetector.detectAnomalies(event));
            
            // If any detector found anomalies, create result
            if (!allDetections.isEmpty()) {
                AnomalyResult result = new AnomalyResult(
                    event.getEventId(),
                    event.getServiceName(),
                    event.getTimestamp()
                );
                
                for (AnomalyResult.DetectionInfo detection : allDetections) {
                    result.addDetection(
                        detection.getMethod(),
                        detection.getMetric(),
                        detection.getScore(),
                        detection.getThreshold()
                    );
                }
                
                result.setOriginalEvent(event);
                
                // Store in Redis cache
                cacheAnomaly(result);
                
                // Store in PostgreSQL
                persistAnomaly(result);
                
                // Send to Kafka for downstream processing
                String resultJson = objectMapper.writeValueAsString(result);
                kafkaTemplate.send(ANOMALY_TOPIC, result.getEventId(), resultJson);
                
                logger.info("Anomaly detected: eventId={}, confidence={}, detections={}", 
                           result.getEventId(), result.getConfidence(), result.getDetections().size());
            }
        } catch (Exception e) {
            logger.error("Error processing log event", e);
        }
    }
    
    private void cacheAnomaly(AnomalyResult result) {
        try {
            String key = "anomaly:" + result.getEventId();
            redisTemplate.opsForValue().set(key, result, Duration.ofHours(24));
        } catch (Exception e) {
            logger.error("Failed to cache anomaly", e);
        }
    }
    
    private void persistAnomaly(AnomalyResult result) {
        try {
            AnomalyEntity entity = new AnomalyEntity();
            entity.setEventId(result.getEventId());
            entity.setServiceName(result.getServiceName());
            entity.setTimestamp(result.getTimestamp());
            entity.setConfidence(result.getConfidence());
            entity.setDetectionCount(result.getDetections().size());
            entity.setRawData(objectMapper.writeValueAsString(result));
            
            anomalyRepository.save(entity);
        } catch (Exception e) {
            logger.error("Failed to persist anomaly", e);
        }
    }
    
    public void fallbackDetection(String message, Exception e) {
        logger.error("Circuit breaker activated for anomaly detection", e);
    }
}
