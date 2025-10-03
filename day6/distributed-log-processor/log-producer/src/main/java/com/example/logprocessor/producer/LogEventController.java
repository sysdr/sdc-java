package com.example.logprocessor.producer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/logs")
public class LogEventController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventController.class);
    
    private final KafkaProducerService kafkaProducerService;
    private final Counter logsReceivedCounter;
    private final Counter logsSentCounter;
    private final Counter logsFailedCounter;

    public LogEventController(KafkaProducerService kafkaProducerService, MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        this.logsReceivedCounter = Counter.builder("logs_received_total")
                .description("Total number of log events received")
                .register(meterRegistry);
        this.logsSentCounter = Counter.builder("logs_sent_total")
                .description("Total number of log events sent to Kafka")
                .register(meterRegistry);
        this.logsFailedCounter = Counter.builder("logs_failed_total")
                .description("Total number of log events that failed to send")
                .register(meterRegistry);
    }

    @PostMapping
    @Timed(value = "log_ingestion_duration", description = "Time taken to ingest log events")
    public ResponseEntity<String> ingestLog(@Valid @RequestBody LogEvent logEvent) {
        logsReceivedCounter.increment();
        
        try {
            kafkaProducerService.sendLogEvent(logEvent);
            logsSentCounter.increment();
            logger.debug("Log event sent successfully: {}", logEvent.message());
            return ResponseEntity.accepted().body("Log event accepted for processing");
        } catch (Exception e) {
            logsFailedCounter.increment();
            logger.error("Failed to send log event: {}", logEvent.message(), e);
            return ResponseEntity.internalServerError().body("Failed to process log event");
        }
    }

    @PostMapping("/batch")
    @Timed(value = "batch_log_ingestion_duration", description = "Time taken to ingest batch log events")
    public ResponseEntity<String> ingestLogsBatch(@Valid @RequestBody List<LogEvent> logEvents) {
        logsReceivedCounter.increment(logEvents.size());
        
        try {
            CompletableFuture<Void> future = kafkaProducerService.sendLogEventsBatch(logEvents);
            future.thenRun(() -> {
                logsSentCounter.increment(logEvents.size());
                logger.debug("Batch of {} log events sent successfully", logEvents.size());
            }).exceptionally(throwable -> {
                logsFailedCounter.increment(logEvents.size());
                logger.error("Failed to send batch of log events", throwable);
                return null;
            });
            
            return ResponseEntity.accepted().body("Batch of log events accepted for processing");
        } catch (Exception e) {
            logsFailedCounter.increment(logEvents.size());
            logger.error("Failed to send batch of log events", e);
            return ResponseEntity.internalServerError().body("Failed to process batch of log events");
        }
    }
}
