package com.example.logprocessor.producer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogEventController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventController.class);
    
    private final KafkaProducerService kafkaProducerService;
    private final Counter logIngestCounter;
    private final Timer logIngestTimer;
    private final MeterRegistry meterRegistry;
    
    public LogEventController(KafkaProducerService kafkaProducerService, MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        this.meterRegistry = meterRegistry;
        this.logIngestCounter = Counter.builder("log_ingestion_total")
                .description("Total number of logs ingested")
                .register(meterRegistry);
        this.logIngestTimer = Timer.builder("log_ingestion_duration")
                .description("Time spent ingesting logs")
                .register(meterRegistry);
    }
    
    @PostMapping("/ingest")
    @CircuitBreaker(name = "log-ingestion", fallbackMethod = "fallbackIngest")
    @RateLimiter(name = "log-ingestion")
    public ResponseEntity<Map<String, String>> ingestLog(@Valid @RequestBody LogEvent logEvent) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            logger.info("Ingesting log event: {}", logEvent.getId());
            
            CompletableFuture<Void> future = kafkaProducerService.sendLogEvent(logEvent);
            future.get(); // Wait for acknowledgment in synchronous endpoint
            
            logIngestCounter.increment();
            
            return ResponseEntity.ok(Map.of(
                    "status", "accepted",
                    "logId", logEvent.getId(),
                    "message", "Log event successfully ingested"
            ));
        } catch (Exception e) {
            logger.error("Failed to ingest log event: {}", logEvent.getId(), e);
            throw new RuntimeException("Failed to process log event", e);
        } finally {
            sample.stop(logIngestTimer);
        }
    }
    
    @PostMapping("/ingest/async")
    @CircuitBreaker(name = "log-ingestion", fallbackMethod = "fallbackIngest")
    @RateLimiter(name = "log-ingestion")
    public ResponseEntity<Map<String, String>> ingestLogAsync(@Valid @RequestBody LogEvent logEvent) {
        try {
            logger.info("Ingesting log event asynchronously: {}", logEvent.getId());
            
            kafkaProducerService.sendLogEvent(logEvent)
                    .thenRun(() -> {
                        logIngestCounter.increment();
                        logger.info("Log event {} processed successfully", logEvent.getId());
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to process log event: {}", logEvent.getId(), throwable);
                        return null;
                    });
            
            return ResponseEntity.accepted().body(Map.of(
                    "status", "accepted",
                    "logId", logEvent.getId(),
                    "message", "Log event queued for processing"
            ));
        } catch (Exception e) {
            logger.error("Failed to queue log event: {}", logEvent.getId(), e);
            throw new RuntimeException("Failed to queue log event", e);
        }
    }
    
    @PostMapping("/batch")
    @CircuitBreaker(name = "log-ingestion", fallbackMethod = "fallbackBatchIngest")
    @RateLimiter(name = "batch-ingestion")
    public ResponseEntity<Map<String, Object>> ingestLogBatch(@Valid @RequestBody LogEvent[] logEvents) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            logger.info("Ingesting batch of {} log events", logEvents.length);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (LogEvent logEvent : logEvents) {
                try {
                    kafkaProducerService.sendLogEvent(logEvent).get();
                    successCount++;
                    logIngestCounter.increment();
                } catch (Exception e) {
                    logger.warn("Failed to ingest log event in batch: {}", logEvent.getId(), e);
                    failureCount++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "totalEvents", logEvents.length,
                    "successCount", successCount,
                    "failureCount", failureCount
            ));
        } catch (Exception e) {
            logger.error("Failed to process log batch", e);
            throw new RuntimeException("Failed to process log batch", e);
        } finally {
            sample.stop(logIngestTimer);
        }
    }
    
    // Circuit breaker fallback methods
    public ResponseEntity<Map<String, String>> fallbackIngest(LogEvent logEvent, Exception ex) {
        logger.warn("Circuit breaker activated for log ingestion: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "unavailable",
                        "logId", logEvent.getId(),
                        "message", "Log ingestion service temporarily unavailable"
                ));
    }
    
    public ResponseEntity<Map<String, Object>> fallbackBatchIngest(LogEvent[] logEvents, Exception ex) {
        logger.warn("Circuit breaker activated for batch log ingestion: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "unavailable",
                        "totalEvents", logEvents.length,
                        "message", "Batch log ingestion service temporarily unavailable"
                ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "log-producer",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
