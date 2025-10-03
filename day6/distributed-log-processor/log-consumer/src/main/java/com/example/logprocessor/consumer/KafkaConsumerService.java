package com.example.logprocessor.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    private final Counter failedCounter;

    public KafkaConsumerService(LogEntryRepository logEntryRepository, 
                               ObjectMapper objectMapper, 
                               MeterRegistry meterRegistry) {
        this.logEntryRepository = logEntryRepository;
        this.objectMapper = objectMapper;
        this.processedCounter = Counter.builder("logs_processed_total")
                .description("Total number of log events processed")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("logs_processing_failed_total")
                .description("Total number of log events that failed processing")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    @Transactional
    public void consumeLogEvent(@Payload String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               Acknowledgment acknowledgment) {
        
        logger.debug("Received message from topic=[{}], partition=[{}], offset=[{}]", topic, partition, offset);
        
        try {
            LogEventDto logEventDto = objectMapper.readValue(message, LogEventDto.class);
            
            // Convert metadata to String map for JPA storage
            Map<String, String> stringMetadata = new HashMap<>();
            if (logEventDto.metadata() != null) {
                logEventDto.metadata().forEach((key, value) -> 
                    stringMetadata.put(key, value != null ? value.toString() : null));
            }
            
            LogEntry logEntry = new LogEntry(
                logEventDto.message(),
                logEventDto.level(),
                logEventDto.source(),
                logEventDto.timestamp(),
                stringMetadata
            );
            
            logEntryRepository.save(logEntry);
            processedCounter.increment();
            
            logger.debug("Successfully processed log entry: {}", logEntry.getId());
            acknowledgment.acknowledge();
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse log event JSON: {}", message, e);
            failedCounter.increment();
            acknowledgment.acknowledge(); // Acknowledge to prevent reprocessing of poison message
        } catch (Exception e) {
            logger.error("Failed to process log event: {}", message, e);
            failedCounter.increment();
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process log event", e);
        }
    }

    // Internal DTO for deserialization
    private record LogEventDto(
        String message,
        String level,
        String source,
        Instant timestamp,
        Map<String, Object> metadata
    ) {}
}
