package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class LogProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(LogProcessingService.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public LogProcessingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Transactional
    public void processNormal(LogEvent event) {
        String timestampKey = "queue:normal:" + event.getId();
        redisTemplate.opsForValue().set(timestampKey, 
            String.valueOf(event.getTimestamp().toEpochMilli()), 60, TimeUnit.SECONDS);
        
        entityManager.persist(event);
        logger.debug("Processed normal log {}", event.getId());
    }
    
    @Transactional
    public void processLow(LogEvent event) {
        // Store timestamp for escalation monitoring
        String timestampKey = "queue:low:" + event.getId();
        redisTemplate.opsForValue().set(timestampKey, 
            String.valueOf(event.getTimestamp().toEpochMilli()), 120, TimeUnit.SECONDS);
        
        // Batch processing for low priority logs
        entityManager.persist(event);
        logger.trace("Processed low priority log {}", event.getId());
    }
}
