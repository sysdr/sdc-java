package com.systemdesign.logprocessor.gateway.controller;

import com.systemdesign.logprocessor.model.ConsumerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("redis", checkRedis());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics/consumer")
    public ResponseEntity<ConsumerMetrics> getConsumerMetrics() {
        // In production, this would aggregate metrics from all consumer instances
        ConsumerMetrics metrics = ConsumerMetrics.builder()
            .groupId("log-processor-group")
            .activeConsumers(3)
            .partitionLags(new HashMap<>())
            .totalProcessed(getTotalProcessed())
            .totalFailed(getTotalFailed())
            .averageProcessingTimeMs(getAverageProcessingTime())
            .rebalanceCount(0)
            .build();
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM metrics
        metrics.put("jvm_memory_used", 
            meterRegistry.get("jvm.memory.used").gauge().value());
        metrics.put("jvm_threads", 
            meterRegistry.get("jvm.threads.live").gauge().value());
        
        // Custom application metrics
        metrics.put("logs_processed", getTotalProcessed());
        metrics.put("processing_errors", getTotalFailed());
        
        return ResponseEntity.ok(metrics);
    }

    private String checkRedis() {
        try {
            redisTemplate.opsForValue().set("health:check", "ok", 1, TimeUnit.SECONDS);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private long getTotalProcessed() {
        String value = redisTemplate.opsForValue().get("metrics:total_processed");
        return value != null ? Long.parseLong(value) : 0;
    }

    private long getTotalFailed() {
        String value = redisTemplate.opsForValue().get("metrics:total_failed");
        return value != null ? Long.parseLong(value) : 0;
    }

    private double getAverageProcessingTime() {
        try {
            return meterRegistry.get("log.processing.duration")
                .timer()
                .mean(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
