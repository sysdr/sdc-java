package com.example.consumer.service;

import com.example.consumer.model.LogEvent;
import com.example.consumer.model.ProcessedLog;
import com.example.consumer.repository.ProcessedLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class TransactionalLogProcessor {

    private final ProcessedLogRepository repository;
    private final IdempotencyService idempotencyService;
    private final Counter processedCounter;
    private final Counter duplicateCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;

    public TransactionalLogProcessor(ProcessedLogRepository repository,
                                    IdempotencyService idempotencyService,
                                    MeterRegistry meterRegistry) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.processedCounter = Counter.builder("log_consumer.processed")
                .description("Successfully processed events")
                .register(meterRegistry);
        this.duplicateCounter = Counter.builder("log_consumer.duplicates")
                .description("Duplicate events skipped")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("log_consumer.errors")
                .description("Processing errors")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("log_consumer.processing.duration")
                .description("Event processing duration")
                .register(meterRegistry);
    }

    /**
     * Process log events with exactly-once guarantees
     * Transaction includes: idempotency check, database write, and offset commit
     */
    @KafkaListener(topics = "raw-logs", groupId = "log-processor-group")
    @Transactional
    public void processLog(ConsumerRecord<String, LogEvent> record, 
                          Acknowledgment acknowledgment) {
        
        processingTimer.record(() -> {
            LogEvent event = record.value();
            
            try {
                // Step 1: Check idempotency (distributed deduplication)
                long timestamp = event.getTimestamp() != null ? 
                        event.getTimestamp().toEpochMilli() : System.currentTimeMillis();
                
                if (!idempotencyService.tryAcquire(event.getEventId(), timestamp)) {
                    duplicateCounter.increment();
                    log.info("Skipping duplicate event: {} from partition {} offset {}", 
                            event.getEventId(), record.partition(), record.offset());
                    acknowledgment.acknowledge();
                    return;
                }

                // Step 2: Check database-level deduplication (defensive)
                if (repository.existsByEventId(event.getEventId())) {
                    duplicateCounter.increment();
                    log.warn("Event {} already in database, skipping", event.getEventId());
                    idempotencyService.markProcessed(event.getEventId(), timestamp);
                    acknowledgment.acknowledge();
                    return;
                }

                // Step 3: Process and persist
                ProcessedLog processedLog = ProcessedLog.builder()
                        .eventId(event.getEventId())
                        .eventType(event.getEventType())
                        .service(event.getService())
                        .message(event.getMessage())
                        .severity(event.getSeverity())
                        .eventTimestamp(event.getTimestamp())
                        .processedAt(Instant.now())
                        .userId(event.getUserId())
                        .traceId(event.getTraceId())
                        .partition(record.partition())
                        .offset(record.offset())
                        .build();

                repository.save(processedLog);
                
                // Step 4: Mark as processed in Redis
                idempotencyService.markProcessed(event.getEventId(), timestamp);
                
                // Step 5: Commit offset (part of transaction)
                acknowledgment.acknowledge();
                
                processedCounter.increment();
                log.debug("Successfully processed event {} from partition {} offset {}", 
                        event.getEventId(), record.partition(), record.offset());
                
            } catch (Exception e) {
                errorCounter.increment();
                log.error("Error processing event {}: {}", event.getEventId(), e.getMessage(), e);
                // Release idempotency lock to allow retry
                long timestamp = event.getTimestamp() != null ? 
                        event.getTimestamp().toEpochMilli() : System.currentTimeMillis();
                idempotencyService.release(event.getEventId(), timestamp);
                throw e; // Rollback transaction
            }
        });
    }
}
