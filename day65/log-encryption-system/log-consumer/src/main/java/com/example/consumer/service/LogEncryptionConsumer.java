package com.example.consumer.service;

import com.example.consumer.entity.StoredLogEvent;
import com.example.consumer.model.EncryptedField;
import com.example.consumer.model.LogEvent;
import com.example.consumer.repository.LogEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Consumes raw log events, encrypts PII fields, and stores to PostgreSQL.
 * Uses async processing with batching for high throughput.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogEncryptionConsumer {
    
    private final EncryptionClient encryptionClient;
    private final LogEventRepository logEventRepository;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    private final Counter encryptionErrorCounter;
    
    public LogEncryptionConsumer(EncryptionClient encryptionClient,
                                LogEventRepository logEventRepository,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.encryptionClient = encryptionClient;
        this.logEventRepository = logEventRepository;
        this.objectMapper = objectMapper;
        
        this.processedCounter = Counter.builder("log.events.processed.total")
            .description("Total log events processed")
            .register(meterRegistry);
        
        this.encryptionErrorCounter = Counter.builder("log.encryption.errors.total")
            .description("Total encryption errors")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "raw-logs", containerFactory = "kafkaListenerContainerFactory")
    public void processLogs(List<LogEvent> events, Acknowledgment acknowledgment) {
        log.info("Processing batch of {} log events", events.size());
        
        try {
            // Process events asynchronously in parallel
            List<CompletableFuture<StoredLogEvent>> futures = events.stream()
                .map(this::processEventAsync)
                .collect(Collectors.toList());
            
            // Wait for all encryption operations to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<StoredLogEvent> storedEvents = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());
                    
                    // Batch insert to database
                    logEventRepository.saveAll(storedEvents);
                    processedCounter.increment(events.size());
                    
                    log.info("Successfully stored {} encrypted log events", storedEvents.size());
                })
                .exceptionally(ex -> {
                    log.error("Failed to process log batch", ex);
                    encryptionErrorCounter.increment();
                    return null;
                })
                .join();
            
            // Acknowledge after successful processing
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing log batch", e);
            encryptionErrorCounter.increment();
        }
    }
    
    @Async
    public CompletableFuture<StoredLogEvent> processEventAsync(LogEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StoredLogEvent storedEvent = new StoredLogEvent();
                storedEvent.setEventId(event.getEventId());
                storedEvent.setEventType(event.getEventType());
                storedEvent.setTimestamp(event.getTimestamp());
                storedEvent.setSeverity(event.getSeverity());
                storedEvent.setCreatedAt(Instant.now());
                
                // Store public fields as-is
                storedEvent.setPublicFields(objectMapper.writeValueAsString(event.getPublicFields()));
                
                // Encrypt PII fields
                if (event.getPiiFields() != null && !event.getPiiFields().isEmpty()) {
                    List<EncryptedField> encryptedFields = encryptionClient.encryptFields(event.getPiiFields());
                    storedEvent.setEncryptedFields(objectMapper.writeValueAsString(encryptedFields));
                }
                
                storedEvent.setMetadata(objectMapper.writeValueAsString(event.getMetadata()));
                
                return storedEvent;
                
            } catch (Exception e) {
                log.error("Failed to process event: {}", event.getEventId(), e);
                throw new RuntimeException("Event processing failed", e);
            }
        });
    }
}
