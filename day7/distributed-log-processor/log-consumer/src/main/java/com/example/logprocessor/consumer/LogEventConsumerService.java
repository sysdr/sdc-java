package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LogEventConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumerService.class);
    
    private final LogEventRepository logEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Counter consumedCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    
    public LogEventConsumerService(LogEventRepository logEventRepository, MeterRegistry meterRegistry) {
        this.logEventRepository = logEventRepository;
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        this.consumedCounter = Counter.builder("kafka_messages_consumed_total")
                .description("Total number of Kafka messages consumed")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("kafka_consumption_errors_total")
                .description("Total number of Kafka consumption errors")
                .register(meterRegistry);
        
        this.processingTimer = Timer.builder("log_processing_duration")
                .description("Time spent processing log events")
                .register(meterRegistry);
    }
    
    @KafkaListener(
            topics = "${app.kafka.log-events-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "log-processing", fallbackMethod = "fallbackProcessLogEvent")
    @Retry(name = "log-processing")
    public void processLogEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.debug("Processing log event from topic: {}, partition: {}, offset: {}", 
                        topic, partition, offset);
            
            JsonNode jsonNode = objectMapper.readTree(message);
            
            LogEventEntity logEvent = new LogEventEntity(
                    jsonNode.get("id").asText(),
                    jsonNode.get("level").asText(),
                    jsonNode.get("message").asText(),
                    jsonNode.get("source").asText(),
                    LocalDateTime.parse(jsonNode.get("timestamp").asText()),
                    parseMetadata(jsonNode.get("metadata"))
            );
            
            persistLogEvent(logEvent);
            
            consumedCounter.increment();
            acknowledgment.acknowledge();
            
            logger.debug("Successfully processed log event: {}", logEvent.getId());
            
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate key violations (idempotency)
            logger.debug("Duplicate log event detected, skipping: {}", e.getMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            errorCounter.increment();
            logger.error("Failed to process log event from offset {}: {}", offset, e.getMessage(), e);
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Failed to process log event", e);
        } finally {
            sample.stop(Timer.builder("log_processing_duration").register(meterRegistry));
        }
    }
    
    @Transactional
    public void persistLogEvent(LogEventEntity logEvent) {
        try {
            logEventRepository.save(logEvent);
            logger.debug("Persisted log event: {}", logEvent.getId());
        } catch (DataIntegrityViolationException e) {
            // Log event already exists (idempotent processing)
            logger.debug("Log event {} already exists, skipping", logEvent.getId());
        } catch (Exception e) {
            logger.error("Failed to persist log event: {}", logEvent.getId(), e);
            throw e;
        }
    }
    
    private Map<String, Object> parseMetadata(JsonNode metadataNode) {
        if (metadataNode == null || metadataNode.isNull()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.convertValue(metadataNode, Map.class);
        } catch (Exception e) {
            logger.warn("Failed to parse metadata, returning empty map", e);
            return new HashMap<>();
        }
    }
    
    // Circuit breaker fallback method
    public void fallbackProcessLogEvent(
            String message, String topic, int partition, long offset, 
            Acknowledgment acknowledgment, Exception ex
    ) {
        logger.error("Circuit breaker activated for log processing. " +
                    "Topic: {}, Partition: {}, Offset: {}, Error: {}", 
                    topic, partition, offset, ex.getMessage());
        
        // Don't acknowledge - let Kafka retry later when circuit is closed
        errorCounter.increment();
    }
}
