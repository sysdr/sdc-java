package com.example.logprocessor.producer.controller;

import com.example.logprocessor.producer.model.LogEvent;
import com.example.logprocessor.producer.service.KafkaProducerService;
import com.example.logprocessor.producer.service.WriteAheadLogService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
public class LogEventController {

    private static final Logger logger = LoggerFactory.getLogger(LogEventController.class);
    
    private final KafkaProducerService kafkaProducerService;
    private final WriteAheadLogService walService;
    private final Counter logsReceivedCounter;
    private final Counter logsProcessedCounter;
    private final Counter logsRejectedCounter;
    private final Timer processingTimer;

    @Autowired
    public LogEventController(KafkaProducerService kafkaProducerService, 
                             WriteAheadLogService walService,
                             MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        this.walService = walService;
        this.logsReceivedCounter = Counter.builder("logs_received_total")
                .description("Total number of log events received")
                .register(meterRegistry);
        this.logsProcessedCounter = Counter.builder("logs_processed_total")
                .description("Total number of log events processed successfully")
                .register(meterRegistry);
        this.logsRejectedCounter = Counter.builder("logs_rejected_total")
                .description("Total number of log events rejected")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("log_processing_duration")
                .description("Time taken to process log events")
                .register(meterRegistry);
    }

    @PostMapping
    @CircuitBreaker(name = "log-ingestion", fallbackMethod = "fallbackLogIngestion")
    @RateLimiter(name = "log-ingestion")
    public ResponseEntity<String> ingestLog(@RequestBody LogEvent logEvent) {
        Sample sample = Timer.start();
        try {
            logsReceivedCounter.increment();
            
            // Generate trace ID if not present
            if (logEvent.getTraceId() == null) {
                logEvent.setTraceId(UUID.randomUUID().toString());
            }

            logger.debug("Processing log event: {}", logEvent);

            // Write to WAL for durability
            walService.append(logEvent);

            // Send to Kafka for async processing
            kafkaProducerService.sendLogEvent(logEvent);

            logsProcessedCounter.increment();
            logger.debug("Successfully processed log event with trace ID: {}", logEvent.getTraceId());

            return ResponseEntity.accepted()
                    .header("X-Trace-Id", logEvent.getTraceId())
                    .body("Log event accepted for processing");

        } catch (Exception e) {
            logsRejectedCounter.increment();
            logger.error("Failed to process log event", e);
            throw new RuntimeException(e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    @PostMapping("/batch")
    @CircuitBreaker(name = "log-ingestion", fallbackMethod = "fallbackBatchLogIngestion")
    @RateLimiter(name = "batch-log-ingestion")
    public ResponseEntity<String> ingestBatchLogs(@RequestBody LogEvent[] logEvents) {
        Sample sample = Timer.start();
        try {
            logsReceivedCounter.increment(logEvents.length);
            
            for (LogEvent logEvent : logEvents) {
                if (logEvent.getTraceId() == null) {
                    logEvent.setTraceId(UUID.randomUUID().toString());
                }
                walService.append(logEvent);
                kafkaProducerService.sendLogEvent(logEvent);
            }

            logsProcessedCounter.increment(logEvents.length);
            logger.info("Successfully processed batch of {} log events", logEvents.length);

            return ResponseEntity.accepted()
                    .body("Batch of " + logEvents.length + " log events accepted for processing");

        } catch (Exception e) {
            logsRejectedCounter.increment(logEvents.length);
            logger.error("Failed to process batch log events", e);
            throw new RuntimeException(e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    // Circuit breaker fallback methods
    public ResponseEntity<String> fallbackLogIngestion(LogEvent logEvent, Exception ex) {
        logger.warn("Circuit breaker activated for log ingestion. Fallback triggered.", ex);
        logsRejectedCounter.increment();
        return ResponseEntity.status(503)
                .body("Service temporarily unavailable. Please retry later.");
    }

    public ResponseEntity<String> fallbackBatchLogIngestion(LogEvent[] logEvents, Exception ex) {
        logger.warn("Circuit breaker activated for batch log ingestion. Fallback triggered.", ex);
        logsRejectedCounter.increment(logEvents.length);
        return ResponseEntity.status(503)
                .body("Service temporarily unavailable. Please retry later.");
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Log Producer Service is healthy");
    }
}
