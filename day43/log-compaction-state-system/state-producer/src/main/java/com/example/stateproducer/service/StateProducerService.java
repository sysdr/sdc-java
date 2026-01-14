package com.example.stateproducer.service;

import com.example.stateproducer.model.EntityState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StateProducerService {

    private final KafkaTemplate<String, EntityState> kafkaTemplate;
    private final Counter stateUpdatesCounter;
    private final Counter stateDeletionsCounter;
    private final Counter stateUpdateErrorsCounter;

    @Value("${kafka.topic.entity-state}")
    private String entityStateTopic;

    public StateProducerService(KafkaTemplate<String, EntityState> kafkaTemplate,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.stateUpdatesCounter = Counter.builder("state.updates.total")
                .description("Total state updates sent")
                .register(meterRegistry);
        this.stateDeletionsCounter = Counter.builder("state.deletions.total")
                .description("Total state deletions (tombstones) sent")
                .register(meterRegistry);
        this.stateUpdateErrorsCounter = Counter.builder("state.update.errors")
                .description("State update errors")
                .register(meterRegistry);
    }

    /**
     * Update entity state. Uses entity ID as key for compaction.
     */
    public CompletableFuture<SendResult<String, EntityState>> updateEntityState(EntityState state) {
        String key = buildKey(state.getEntityId(), state.getEntityType());
        state.setTimestamp(Instant.now());
        
        log.debug("Updating entity state: {} with key: {}", state.getEntityId(), key);
        
        CompletableFuture<SendResult<String, EntityState>> future = 
            kafkaTemplate.send(entityStateTopic, key, state);
            
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                stateUpdatesCounter.increment();
                log.info("State update successful: {} -> partition {}, offset {}", 
                    key, result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                stateUpdateErrorsCounter.increment();
                log.error("State update failed for key: {}", key, ex);
            }
        });
        
        return future;
    }

    /**
     * Delete entity state by sending tombstone (null value).
     * Critical for compaction: tombstone tells Kafka to eventually remove all
     * previous versions of this key from the log.
     */
    public CompletableFuture<SendResult<String, EntityState>> deleteEntityState(
            String entityId, String entityType) {
        String key = buildKey(entityId, entityType);
        
        log.info("Sending tombstone for entity: {} (key: {})", entityId, key);
        
        // Tombstone: key present, value null
        CompletableFuture<SendResult<String, EntityState>> future = 
            kafkaTemplate.send(entityStateTopic, key, null);
            
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                stateDeletionsCounter.increment();
                log.info("Tombstone sent successfully: {} -> partition {}, offset {}", 
                    key, result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                stateUpdateErrorsCounter.increment();
                log.error("Tombstone send failed for key: {}", key, ex);
            }
        });
        
        return future;
    }

    /**
     * Synchronous state update with timeout.
     * Use for critical state changes that must be confirmed.
     */
    public void updateEntityStateSync(EntityState state, long timeoutSeconds) {
        try {
            updateEntityState(state).get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Synchronous state update completed: {}", state.getEntityId());
        } catch (Exception e) {
            log.error("Synchronous state update failed: {}", state.getEntityId(), e);
            throw new RuntimeException("Failed to update entity state", e);
        }
    }

    private String buildKey(String entityId, String entityType) {
        // Key format: type:id ensures proper partitioning and compaction
        return String.format("%s:%s", entityType, entityId);
    }
}
