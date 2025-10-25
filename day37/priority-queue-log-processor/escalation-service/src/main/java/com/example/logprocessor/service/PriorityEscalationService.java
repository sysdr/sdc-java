package com.example.logprocessor.service;

import com.example.logprocessor.model.PriorityLevel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class PriorityEscalationService {
    private static final Logger logger = LoggerFactory.getLogger(PriorityEscalationService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<PriorityLevel, Counter> escalationCounters;
    
    public PriorityEscalationService(RedisTemplate<String, String> redisTemplate,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        
        this.escalationCounters = new EnumMap<>(PriorityLevel.class);
        for (PriorityLevel level : PriorityLevel.values()) {
            escalationCounters.put(level,
                Counter.builder("priority.escalations")
                    .tag("from_priority", level.name())
                    .register(meterRegistry));
        }
    }
    
    /**
     * Check for aged messages every 10 seconds and escalate if needed
     */
    @Scheduled(fixedRate = 10000)
    public void checkAndEscalate() {
        long now = System.currentTimeMillis();
        
        // Check each priority level (except CRITICAL which can't escalate)
        for (PriorityLevel priority : new PriorityLevel[]{PriorityLevel.LOW, 
                                                           PriorityLevel.NORMAL, 
                                                           PriorityLevel.HIGH}) {
            String queueKey = "queue:" + priority.name().toLowerCase() + ":*";
            Set<String> keys = redisTemplate.keys(queueKey);
            
            if (keys == null || keys.isEmpty()) {
                continue;
            }
            
            for (String key : keys) {
                String timestampStr = redisTemplate.opsForValue().get(key);
                if (timestampStr == null) continue;
                
                try {
                    long timestamp = Long.parseLong(timestampStr);
                    long age = now - timestamp;
                    
                    if (age > priority.getEscalationThresholdMs()) {
                        escalateMessage(key, priority);
                    }
                } catch (NumberFormatException e) {
                    logger.error("Invalid timestamp for key {}", key, e);
                }
            }
        }
    }
    
    private void escalateMessage(String redisKey, PriorityLevel currentPriority) {
        // Extract message ID from Redis key (format: queue:normal:messageId)
        String messageId = redisKey.substring(redisKey.lastIndexOf(':') + 1);
        
        PriorityLevel newPriority = currentPriority.escalate();
        
        logger.warn("⬆️  ESCALATING message {} from {} to {}", 
            messageId, currentPriority, newPriority);
        
        // In a real system, we would:
        // 1. Fetch the original message from the old topic or database
        // 2. Republish to the new priority topic
        // 3. Delete from the old queue
        
        // For this demo, we just log and update metrics
        escalationCounters.get(currentPriority).increment();
        
        // Remove from current priority queue
        redisTemplate.delete(redisKey);
        
        // Add to higher priority queue (simulated)
        String newKey = "queue:" + newPriority.name().toLowerCase() + ":" + messageId;
        redisTemplate.opsForValue().set(newKey, String.valueOf(System.currentTimeMillis()), 60, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    /**
     * Log escalation statistics
     */
    @Scheduled(fixedRate = 60000)
    public void logStatistics() {
        logger.info("=== Priority Escalation Statistics ===");
        for (PriorityLevel level : PriorityLevel.values()) {
            if (level != PriorityLevel.CRITICAL) {
                double count = escalationCounters.get(level).count();
                logger.info("{} escalations: {}", level, count);
            }
        }
    }
}
