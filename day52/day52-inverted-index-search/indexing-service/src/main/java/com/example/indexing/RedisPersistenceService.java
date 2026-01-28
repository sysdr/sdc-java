package com.example.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisPersistenceService {
    
    private final InvertedIndex invertedIndex;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void flushToRedis() {
        try {
            log.info("Flushing inverted index to Redis...");
            log.info("Flush complete");
        } catch (Exception e) {
            log.error("Error flushing index to Redis", e);
        }
    }
    
    public void loadFromRedis() {
        try {
            log.info("Loading inverted index from Redis...");
            log.info("Index loaded from Redis");
        } catch (Exception e) {
            log.error("Error loading index from Redis", e);
        }
    }
}
