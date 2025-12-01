package com.example.logprocessor.gateway;

import com.example.logprocessor.common.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogIngestionController {
    
    private final KafkaProducerService kafkaProducerService;
    private final MeterRegistry meterRegistry;
    
    @PostMapping
    public ResponseEntity<String> ingestLog(@RequestBody LogEvent logEvent) {
        try {
            // Set metadata
            if (logEvent.getId() == null) {
                logEvent.setId(UUID.randomUUID().toString());
            }
            if (logEvent.getTimestamp() == null) {
                logEvent.setTimestamp(Instant.now());
            }
            
            // Send to Kafka
            kafkaProducerService.sendLog(logEvent);
            
            // Metrics
            Counter.builder("logs.ingested")
                .tag("level", logEvent.getLevel())
                .tag("source", logEvent.getSource())
                .register(meterRegistry)
                .increment();
            
            return ResponseEntity.ok(logEvent.getId());
        } catch (Exception e) {
            log.error("Failed to ingest log", e);
            meterRegistry.counter("logs.ingestion.failed").increment();
            return ResponseEntity.internalServerError().body("Failed to ingest log");
        }
    }
    
    @PostMapping("/batch")
    public ResponseEntity<String> ingestBatch(@RequestBody List<LogEvent> logs) {
        try {
            int successCount = 0;
            for (LogEvent log : logs) {
                if (log.getId() == null) {
                    log.setId(UUID.randomUUID().toString());
                }
                if (log.getTimestamp() == null) {
                    log.setTimestamp(Instant.now());
                }
                kafkaProducerService.sendLog(log);
                successCount++;
            }
            
            meterRegistry.counter("logs.batch.ingested").increment(successCount);
            return ResponseEntity.ok("Ingested " + successCount + " logs");
        } catch (Exception e) {
            log.error("Failed to ingest batch", e);
            return ResponseEntity.internalServerError().body("Failed to ingest batch");
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway healthy");
    }
}
