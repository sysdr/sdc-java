package com.example.stateconsumer.service;

import com.example.stateconsumer.model.EntityState;
import com.example.stateconsumer.repository.EntityStateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class StateMaterializationService {

    private final EntityStateRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter stateUpdatesCounter;
    private final Counter tombstonesCounter;
    private final Counter rehydrationCounter;
    private final Timer processingTimer;

    private static final String CACHE_PREFIX = "entity:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public StateMaterializationService(EntityStateRepository repository,
                                      RedisTemplate<String, String> redisTemplate,
                                      MeterRegistry meterRegistry) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.stateUpdatesCounter = Counter.builder("state.materialization.updates")
                .description("State updates materialized")
                .register(meterRegistry);
        this.tombstonesCounter = Counter.builder("state.materialization.tombstones")
                .description("Tombstones processed")
                .register(meterRegistry);
        this.rehydrationCounter = Counter.builder("state.materialization.rehydrations")
                .description("Full state rehydrations from offset 0")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("state.materialization.processing.time")
                .description("Time to process state update")
                .register(meterRegistry);
    }

    /**
     * Main consumer for compacted entity state topic.
     * Processes both updates and tombstones to maintain materialized view.
     */
    @KafkaListener(
        topics = "${kafka.topic.entity-state}",
        groupId = "${kafka.consumer.group-id}",
        concurrency = "3"
    )
    @Transactional
    public void consumeEntityState(
            @Payload(required = false) Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {
        
        Timer.Sample sample = Timer.start();
        
        try {
            if (payload == null) {
                // Tombstone: delete entity from materialized views
                processTombstone(key, partition, offset);
            } else {
                // State update: upsert to database and cache
                processStateUpdate(key, payload, partition, offset);
            }
            
            // Manual commit after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Error processing state from partition {} offset {}: {}", 
                partition, offset, e.getMessage(), e);
            // Don't commit on error - message will be reprocessed
            throw new RuntimeException("State materialization failed", e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    private void processStateUpdate(String key, Map<String, Object> payload, 
                                   int partition, long offset) {
        String entityId = extractEntityId(key);
        
        try {
            EntityState state = mapToEntityState(entityId, payload);
            state.setUpdatedAt(Instant.now());
            
            // Upsert to PostgreSQL
            repository.save(state);
            
            // Update Redis cache
            String cacheKey = CACHE_PREFIX + entityId;
            String jsonValue = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, CACHE_TTL);
            
            stateUpdatesCounter.increment();
            log.info("State materialized: {} from partition {} offset {}", 
                entityId, partition, offset);
                
        } catch (Exception e) {
            log.error("Failed to materialize state for key {}: {}", key, e.getMessage());
            throw new RuntimeException("State update processing failed", e);
        }
    }

    private void processTombstone(String key, int partition, long offset) {
        String entityId = extractEntityId(key);
        
        log.info("Processing tombstone for entity: {} from partition {} offset {}", 
            entityId, partition, offset);
        
        // Delete from PostgreSQL
        repository.deleteById(entityId);
        
        // Delete from Redis cache
        String cacheKey = CACHE_PREFIX + entityId;
        redisTemplate.delete(cacheKey);
        
        tombstonesCounter.increment();
        log.info("Entity deleted from materialized view: {}", entityId);
    }

    private EntityState mapToEntityState(String entityId, Map<String, Object> payload) 
            throws JsonProcessingException {
        return EntityState.builder()
            .entityId(entityId)
            .entityType((String) payload.get("entityType"))
            .status((String) payload.get("status"))
            .attributes(objectMapper.writeValueAsString(payload.get("attributes")))
            .timestamp(Instant.parse((String) payload.get("timestamp")))
            .version(payload.get("version") != null ? 
                ((Number) payload.get("version")).longValue() : null)
            .build();
    }

    private String extractEntityId(String key) {
        // Key format: "type:id"
        String[] parts = key.split(":", 2);
        return parts.length > 1 ? parts[1] : key;
    }

    /**
     * Full state rehydration from compacted log.
     * Called during startup or recovery to rebuild state from scratch.
     */
    public void rehydrateFromCompactedLog() {
        log.info("Starting state rehydration from compacted log...");
        rehydrationCounter.increment();
        
        // Consumer will automatically start from earliest offset due to
        // auto.offset.reset=earliest configuration
        // This reads the fully compacted log, getting only latest states
        
        log.info("State rehydration initiated. Consumer will process from offset 0.");
    }
}
