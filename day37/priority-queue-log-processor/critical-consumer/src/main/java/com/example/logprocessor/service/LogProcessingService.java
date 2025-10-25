package com.example.logprocessor.service;

import com.example.logprocessor.model.LogEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    public void processCritical(LogEvent event) {
        // Check for duplicates using Redis
        String dedupKey = "processed:" + event.getId();
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(dedupKey, "1", 10, TimeUnit.MINUTES);
        
        if (Boolean.FALSE.equals(isNew)) {
            logger.debug("Duplicate critical log detected: {}", event.getId());
            return;
        }
        
        // Persist to PostgreSQL
        entityManager.persist(event);
        
        // Alert if processing is slow
        Duration processingDelay = Duration.between(event.getTimestamp(), Instant.now());
        if (processingDelay.toMillis() > 1000) {
            logger.error("CRITICAL LOG DELAYED: {}ms delay for {}", 
                processingDelay.toMillis(), event.getId());
        }
        
        logger.info("✅ Processed critical log {} in {}ms", 
            event.getId(), processingDelay.toMillis());
    }
    
    @Transactional
    public void processHigh(LogEvent event) {
        String dedupKey = "processed:" + event.getId();
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(dedupKey, "1", 5, TimeUnit.MINUTES);
        
        if (Boolean.FALSE.equals(isNew)) {
            return;
        }
        
        entityManager.persist(event);
        logger.info("✅ Processed high priority log {}", event.getId());
    }
}
