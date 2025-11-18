package com.example.logprocessor.consumer.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AvroLogEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AvroLogEventListener.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    private Counter eventsConsumedCounter;
    private Counter v1EventsCounter;
    private Counter v2EventsCounter;
    private Counter processingErrorCounter;
    private Timer processingTimer;
    
    // Track events by correlation ID for tracing demonstration
    private final Map<String, AtomicLong> correlationCounts = new ConcurrentHashMap<>();

    public AvroLogEventListener(
            RedisTemplate<String, String> redisTemplate,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        eventsConsumedCounter = Counter.builder("avro.events.consumed")
            .description("Number of Avro events consumed")
            .register(meterRegistry);
            
        v1EventsCounter = Counter.builder("avro.events.v1")
            .description("Number of V1 schema events")
            .register(meterRegistry);
            
        v2EventsCounter = Counter.builder("avro.events.v2")
            .description("Number of V2 schema events")
            .register(meterRegistry);
            
        processingErrorCounter = Counter.builder("avro.events.processing.errors")
            .description("Number of processing errors")
            .register(meterRegistry);
            
        processingTimer = Timer.builder("avro.processing.time")
            .description("Time to process Avro events")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${kafka.topic.logs}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onLogEvent(
            ConsumerRecord<String, GenericRecord> record,
            Acknowledgment acknowledgment) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            GenericRecord event = record.value();
            
            // Extract common fields
            String eventId = event.get("id").toString();
            long timestamp = (long) event.get("timestamp");
            String level = event.get("level").toString();
            String message = event.get("message").toString();
            String source = event.get("source").toString();
            int schemaVersion = (int) event.get("schemaVersion");
            
            // Handle schema-specific fields
            String correlationId = null;
            Map<?, ?> tags = null;
            String spanId = null;
            
            if (schemaVersion >= 2) {
                // V2 fields - using schema evolution
                Object corrIdObj = event.get("correlationId");
                if (corrIdObj != null) {
                    correlationId = corrIdObj.toString();
                    correlationCounts.computeIfAbsent(correlationId, k -> new AtomicLong(0))
                        .incrementAndGet();
                }
                
                Object tagsObj = event.get("tags");
                if (tagsObj instanceof Map) {
                    tags = (Map<?, ?>) tagsObj;
                }
                
                Object spanIdObj = event.get("spanId");
                if (spanIdObj != null) {
                    spanId = spanIdObj.toString();
                }
                
                v2EventsCounter.increment();
            } else {
                v1EventsCounter.increment();
            }
            
            // Process the event
            processLogEvent(eventId, timestamp, level, message, source, 
                          schemaVersion, correlationId, tags, spanId);
            
            eventsConsumedCounter.increment();
            acknowledgment.acknowledge();
            
            logger.debug("Processed event {} (schema v{}) from partition {} offset {}",
                eventId, schemaVersion, record.partition(), record.offset());
                
        } catch (Exception e) {
            processingErrorCounter.increment();
            logger.error("Error processing event: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried
        } finally {
            sample.stop(processingTimer);
        }
    }

    private void processLogEvent(
            String eventId,
            long timestamp,
            String level,
            String message,
            String source,
            int schemaVersion,
            String correlationId,
            Map<?, ?> tags,
            String spanId) {
        
        // Store in Redis for quick access
        String redisKey = "log:" + eventId;
        redisTemplate.opsForHash().put(redisKey, "level", level);
        redisTemplate.opsForHash().put(redisKey, "message", message);
        redisTemplate.opsForHash().put(redisKey, "source", source);
        redisTemplate.opsForHash().put(redisKey, "schemaVersion", String.valueOf(schemaVersion));
        redisTemplate.opsForHash().put(redisKey, "timestamp", String.valueOf(timestamp));
        
        if (correlationId != null) {
            redisTemplate.opsForHash().put(redisKey, "correlationId", correlationId);
            // Also index by correlation ID
            redisTemplate.opsForSet().add("correlation:" + correlationId, eventId);
        }
        
        if (tags != null && !tags.isEmpty()) {
            redisTemplate.opsForHash().put(redisKey, "tags", tags.toString());
        }
        
        // Set TTL
        redisTemplate.expire(redisKey, Duration.ofHours(24));
        
        // Log based on level
        switch (level) {
            case "ERROR", "FATAL" -> logger.error("[{}] {} - {}", source, level, message);
            case "WARN" -> logger.warn("[{}] {} - {}", source, level, message);
            case "INFO" -> logger.info("[{}] {} - {}", source, level, message);
            default -> logger.debug("[{}] {} - {}", source, level, message);
        }
    }

    public Map<String, AtomicLong> getCorrelationCounts() {
        return correlationCounts;
    }
}
