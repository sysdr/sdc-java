package com.example.logprocessor.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OffsetManager {
    private static final Logger logger = LoggerFactory.getLogger(OffsetManager.class);
    private static final String OFFSET_PREFIX = "offset:";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private final ConcurrentHashMap<String, Long> localOffsets = new ConcurrentHashMap<>();

    public void commitOffset(String filePath, long position) {
        try {
            String key = OFFSET_PREFIX + filePath;
            redisTemplate.opsForValue().set(key, String.valueOf(position), Duration.ofHours(24));
            localOffsets.put(filePath, position);
            logger.debug("Committed offset {} for file {}", position, filePath);
        } catch (Exception e) {
            logger.error("Failed to commit offset for file: " + filePath, e);
            // Keep local offset as fallback
            localOffsets.put(filePath, position);
        }
    }

    public long getOffset(String filePath) {
        try {
            String key = OFFSET_PREFIX + filePath;
            String offset = redisTemplate.opsForValue().get(key);
            if (offset != null) {
                long redisOffset = Long.parseLong(offset);
                localOffsets.put(filePath, redisOffset);
                return redisOffset;
            }
        } catch (Exception e) {
            logger.warn("Failed to get offset from Redis for file: " + filePath, e);
        }
        
        // Fallback to local offset
        return localOffsets.getOrDefault(filePath, 0L);
    }

    public boolean isDuplicate(String contentHash) {
        try {
            String key = "processed:" + contentHash;
            Boolean exists = redisTemplate.hasKey(key);
            if (!exists) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(30));
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to check duplicate for hash: " + contentHash, e);
            return false; // Process if unsure
        }
    }
}
