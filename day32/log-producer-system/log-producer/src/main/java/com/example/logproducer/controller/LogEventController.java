package com.example.logproducer.controller;

import com.example.logproducer.model.LogEvent;
import com.example.logproducer.service.KafkaProducerService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogEventController {
    
    private final KafkaProducerService producerService;
    
    @PostMapping
    @RateLimiter(name = "logIngestion")
    public ResponseEntity<LogEventResponse> ingestLog(@Valid @RequestBody LogEvent logEvent) {
        try {
            // Enrich log event with metadata
            enrichLogEvent(logEvent);
            
            // Send asynchronously based on level
            if (logEvent.getLevel() == LogEvent.LogLevel.ERROR || 
                logEvent.getLevel() == LogEvent.LogLevel.FATAL) {
                // Critical logs - wait for acknowledgment
                producerService.sendSync(logEvent);
            } else {
                // Non-critical logs - fire and forget for throughput
                producerService.sendFireAndForget(logEvent);
            }
            
            return ResponseEntity.accepted()
                .body(new LogEventResponse(logEvent.getEventId(), "Accepted", Instant.now()));
                
        } catch (Exception e) {
            log.error("Failed to ingest log event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LogEventResponse(null, "Failed: " + e.getMessage(), Instant.now()));
        }
    }
    
    @PostMapping("/batch")
    @RateLimiter(name = "batchIngestion")
    public ResponseEntity<BatchLogEventResponse> ingestBatch(
            @Valid @RequestBody List<LogEvent> logEvents) {
        
        try {
            int accepted = 0;
            int failed = 0;
            
            for (LogEvent event : logEvents) {
                try {
                    enrichLogEvent(event);
                    producerService.sendFireAndForget(event);
                    accepted++;
                } catch (Exception e) {
                    log.error("Failed to send log event: {}", event.getEventId(), e);
                    failed++;
                }
            }
            
            return ResponseEntity.accepted()
                .body(new BatchLogEventResponse(accepted, failed, Instant.now()));
                
        } catch (Exception e) {
            log.error("Batch ingestion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BatchLogEventResponse(0, logEvents.size(), Instant.now()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiInfo> apiInfo() {
        return ResponseEntity.ok(new ApiInfo(
            "Log Producer API",
            "POST /api/logs - Ingest a single log event",
            "POST /api/logs/batch - Ingest multiple log events",
            "GET /api/logs/health - Check service health and metrics",
            producerService.getMetrics()
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        boolean isHealthy = producerService.isHealthy();
        return ResponseEntity.ok(new HealthStatus(
            isHealthy ? "UP" : "DOWN",
            producerService.getMetrics()
        ));
    }
    
    private void enrichLogEvent(LogEvent logEvent) {
        if (logEvent.getEventId() == null) {
            logEvent.setEventId(UUID.randomUUID().toString());
        }
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(Instant.now());
        }
        if (logEvent.getHostname() == null) {
            try {
                logEvent.setHostname(InetAddress.getLocalHost().getHostName());
            } catch (Exception e) {
                logEvent.setHostname("unknown");
            }
        }
        if (logEvent.getCorrelationId() == null) {
            logEvent.setCorrelationId(UUID.randomUUID().toString());
        }
    }
    
    record LogEventResponse(String eventId, String status, Instant timestamp) {}
    record BatchLogEventResponse(int accepted, int failed, Instant timestamp) {}
    record HealthStatus(String status, Object metrics) {}
    record ApiInfo(String name, String singleEndpoint, String batchEndpoint, String healthEndpoint, Object metrics) {}
}
