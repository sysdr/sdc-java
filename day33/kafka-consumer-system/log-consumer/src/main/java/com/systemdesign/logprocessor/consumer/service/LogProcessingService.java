package com.systemdesign.logprocessor.consumer.service;

import com.systemdesign.logprocessor.consumer.entity.ProcessedLog;
import com.systemdesign.logprocessor.consumer.repository.ProcessedLogRepository;
import com.systemdesign.logprocessor.model.LogEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogProcessingService {

    private final ProcessedLogRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final EnrichmentService enrichmentService;
    private final DeadLetterQueueService dlqService;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final String DEDUP_KEY_PREFIX = "processed:";
    private static final int DEDUP_TTL_MINUTES = 5;

    /**
     * Process a batch of log events with deduplication, enrichment, and persistence.
     * Returns count of successfully processed messages.
     */
    @Transactional
    @CircuitBreaker(name = "logProcessing", fallbackMethod = "processBatchFallback")
    public int processBatch(List<ConsumerRecord<String, LogEvent>> records) {
        List<ProcessedLog> logsToSave = new ArrayList<>();
        int successCount = 0;
        
        for (ConsumerRecord<String, LogEvent> record : records) {
            try {
                LogEvent event = record.value();
                String messageKey = record.key();
                
                // Deduplication check using Redis
                String dedupKey = DEDUP_KEY_PREFIX + messageKey;
                Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL_MINUTES, TimeUnit.MINUTES);
                
                if (Boolean.FALSE.equals(isNew)) {
                    log.debug("Skipping duplicate message: {}", messageKey);
                    continue;
                }
                
                // Enrich log event with additional context
                LogEvent enriched = enrichmentService.enrich(event);
                
                // Convert to entity for persistence
                ProcessedLog processedLog = convertToEntity(enriched, record);
                logsToSave.add(processedLog);
                successCount++;
                
            } catch (Exception e) {
                log.error("Failed to process message {}: {}", record.key(), e.getMessage());
                handleFailedMessage(record, e);
            }
        }
        
        // Batch insert for better performance
        if (!logsToSave.isEmpty()) {
            repository.saveAll(logsToSave);
            log.debug("Persisted {} logs to database", logsToSave.size());
        }
        
        return successCount;
    }

    /**
     * Fallback method when circuit breaker opens due to downstream failures
     */
    public int processBatchFallback(List<ConsumerRecord<String, LogEvent>> records, Exception e) {
        log.warn("⚠️ Circuit breaker open, routing {} messages to DLQ", records.size());
        records.forEach(record -> dlqService.sendToDLQ(record, "circuit_breaker_open"));
        return 0;
    }

    /**
     * Handle individual message failures with retry logic
     */
    private void handleFailedMessage(ConsumerRecord<String, LogEvent> record, Exception error) {
        String failureKey = "failure:" + record.key();
        String attemptsStr = redisTemplate.opsForValue().get(failureKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        
        if (attempts >= MAX_RETRY_ATTEMPTS) {
            log.error("Max retries exceeded for message {}, sending to DLQ", record.key());
            dlqService.sendToDLQ(record, error.getMessage());
            redisTemplate.delete(failureKey);
        } else {
            // Increment failure counter for future retry
            redisTemplate.opsForValue().set(failureKey, String.valueOf(attempts + 1), 
                1, TimeUnit.HOURS);
        }
    }

    private ProcessedLog convertToEntity(LogEvent event, ConsumerRecord<String, LogEvent> record) {
        return ProcessedLog.builder()
            .logId(event.getId())
            .applicationName(event.getApplicationName())
            .level(event.getLevel())
            .message(event.getMessage())
            .timestamp(event.getTimestamp())
            .host(event.getHost())
            .service(event.getService())
            .traceId(event.getTraceId())
            .enrichedData(event.getEnrichedData())
            .partition(record.partition())
            .offset(record.offset())
            .processedAt(Instant.now())
            .build();
    }
}
