package com.example.logproducer.appshipper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    
    private final KafkaProducerService producerService;
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestLog(@RequestBody Map<String, Object> logData) {
        try {
            Counter.builder("logs.received")
                .tag("source", "application")
                .register(meterRegistry)
                .increment();
            
            LogEvent event = LogEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .source((String) logData.getOrDefault("source", "unknown"))
                .level((String) logData.getOrDefault("level", "INFO"))
                .message((String) logData.getOrDefault("message", ""))
                .timestamp(Instant.now())
                .serviceId((String) logData.getOrDefault("serviceId", "app-service"))
                .traceId((String) logData.getOrDefault("traceId", UUID.randomUUID().toString()))
                .metadata(Map.of(
                    "environment", "production",
                    "region", "us-east-1"
                ))
                .build();
            
            producerService.sendLog(event);
            
            return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "eventId", event.getEventId()
            ));
        } catch (Exception e) {
            log.error("Failed to ingest log", e);
            
            Counter.builder("logs.failed")
                .tag("source", "application")
                .register(meterRegistry)
                .increment();
            
            return ResponseEntity.status(503).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            var logs = (java.util.List<Map<String, Object>>) request.get("logs");
            
            int accepted = 0;
            int failed = 0;
            
            for (Map<String, Object> logData : logs) {
                try {
                    LogEvent event = LogEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .source((String) logData.getOrDefault("source", "unknown"))
                        .level((String) logData.getOrDefault("level", "INFO"))
                        .message((String) logData.getOrDefault("message", ""))
                        .timestamp(Instant.now())
                        .serviceId((String) logData.getOrDefault("serviceId", "app-service"))
                        .traceId((String) logData.getOrDefault("traceId", UUID.randomUUID().toString()))
                        .metadata(Map.of())
                        .build();
                    
                    producerService.sendLog(event);
                    accepted++;
                } catch (Exception e) {
                    log.warn("Failed to process log in batch", e);
                    failed++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "completed",
                "accepted", accepted,
                "failed", failed
            ));
        } catch (Exception e) {
            log.error("Batch ingestion failed", e);
            return ResponseEntity.status(503).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
