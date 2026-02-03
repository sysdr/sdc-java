package com.example.logprocessor.service;

import com.example.logprocessor.model.ConsumerState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class StateManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String STATE_KEY = "consumer:state";
    private static final String PROCESSED_MESSAGES_PREFIX = "processed:messages:";
    
    public StateManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }
    
    public void saveState(ConsumerState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(STATE_KEY, json, Duration.ofMinutes(10));
            log.debug("Saved consumer state: {}", state);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize state", e);
        }
    }
    
    public ConsumerState loadState() {
        String json = redisTemplate.opsForValue().get(STATE_KEY);
        if (json == null) {
            log.info("No existing state found, creating fresh state");
            return ConsumerState.builder()
                .partitionOffsets(new HashMap<>())
                .totalMessagesProcessed(0)
                .stateTimestamp(Instant.now())
                .build();
        }
        
        try {
            ConsumerState state = objectMapper.readValue(json, ConsumerState.class);
            log.info("Loaded existing state: {} messages processed", state.getTotalMessagesProcessed());
            return state;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize state", e);
            return null;
        }
    }
    
    public void markMessageProcessed(String messageId, int partition) {
        String key = PROCESSED_MESSAGES_PREFIX + partition;
        redisTemplate.opsForSet().add(key, messageId);
        redisTemplate.expire(key, Duration.ofMinutes(5));
    }
    
    public boolean isMessageProcessed(String messageId, int partition) {
        String key = PROCESSED_MESSAGES_PREFIX + partition;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, messageId);
        return Boolean.TRUE.equals(isMember);
    }
    
    public boolean validateStateConsistency() {
        ConsumerState state = loadState();
        if (state == null) {
            return false;
        }
        
        // Check that state is recent (within last 30 seconds)
        Duration stateAge = Duration.between(state.getStateTimestamp(), Instant.now());
        if (stateAge.getSeconds() > 30) {
            log.warn("State is stale: {} seconds old", stateAge.getSeconds());
            return false;
        }
        
        // Validate partition offsets are non-negative
        for (Map.Entry<Integer, Long> entry : state.getPartitionOffsets().entrySet()) {
            if (entry.getValue() < 0) {
                log.error("Invalid offset for partition {}: {}", entry.getKey(), entry.getValue());
                return false;
            }
        }
        
        log.info("✅ State validation passed");
        return true;
    }
    
    public void recoverState() {
        log.info("Attempting state recovery...");
        // In production, this would restore from Kafka committed offsets
        // For demo, we'll reset to safe defaults
        ConsumerState freshState = ConsumerState.builder()
            .partitionOffsets(new HashMap<>())
            .totalMessagesProcessed(0)
            .stateTimestamp(Instant.now())
            .build();
        saveState(freshState);
        log.info("State recovered with fresh snapshot");
    }
    
    public void updatePartitionOffset(int partition, long offset) {
        ConsumerState state = loadState();
        if (state != null) {
            state.getPartitionOffsets().put(partition, offset);
            state.setStateTimestamp(Instant.now());
            saveState(state);
        }
    }
}
