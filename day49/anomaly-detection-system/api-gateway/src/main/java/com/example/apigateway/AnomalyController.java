package com.example.apigateway;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {
    
    private final AnomalyRepository anomalyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public AnomalyController(AnomalyRepository anomalyRepository, RedisTemplate<String, Object> redisTemplate) {
        this.anomalyRepository = anomalyRepository;
        this.redisTemplate = redisTemplate;
    }
    
    @GetMapping("/{eventId}")
    public ResponseEntity<AnomalyEntity> getAnomaly(@PathVariable String eventId) {
        // Try cache first
        String cacheKey = "anomaly:" + eventId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return ResponseEntity.ok((AnomalyEntity) cached);
        }
        
        // Fallback to database
        return anomalyRepository.findByEventId(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<AnomalyEntity>> getAnomaliesByService(@PathVariable String serviceName) {
        List<AnomalyEntity> anomalies = anomalyRepository.findByServiceNameOrderByTimestampDesc(serviceName);
        return ResponseEntity.ok(anomalies);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<AnomalyEntity>> getRecentAnomalies(
            @RequestParam(value = "hours", defaultValue = "24") int hours,
            @RequestParam(value = "minConfidence", defaultValue = "0.5") double minConfidence) {
        
        long startTime = Instant.now().minus(hours, ChronoUnit.HOURS).toEpochMilli();
        long endTime = Instant.now().toEpochMilli();
        
        List<AnomalyEntity> anomalies = anomalyRepository
            .findByTimestampBetweenOrderByConfidenceDesc(startTime, endTime)
            .stream()
            .filter(a -> a.getConfidence() >= minConfidence)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(anomalies);
    }
    
    @GetMapping("/high-confidence")
    public ResponseEntity<List<AnomalyEntity>> getHighConfidenceAnomalies(
            @RequestParam(value = "minConfidence", defaultValue = "0.7") double minConfidence) {
        
        List<AnomalyEntity> anomalies = anomalyRepository.findHighConfidenceAnomalies(minConfidence);
        return ResponseEntity.ok(anomalies);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<AnomalyStats> getStats() {
        long total = anomalyRepository.count();
        
        long last24h = anomalyRepository.findByTimestampBetweenOrderByConfidenceDesc(
            Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli(),
            Instant.now().toEpochMilli()
        ).size();
        
        List<AnomalyEntity> highConfidence = anomalyRepository.findHighConfidenceAnomalies(0.7);
        
        AnomalyStats stats = new AnomalyStats(total, last24h, highConfidence.size());
        return ResponseEntity.ok(stats);
    }
}
