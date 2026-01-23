package com.example.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class WindowResultCache {
    private static final Logger logger = LoggerFactory.getLogger(WindowResultCache.class);
    private static final String KEY_PREFIX = "window:";
    private static final Duration TTL = Duration.ofHours(2);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public WindowResultCache(RedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void put(WindowResult result) {
        try {
            String key = buildKey(result);
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            logger.error("Failed to cache window result: {}", e.getMessage());
        }
    }
    
    public WindowResult get(String windowKey, String windowType, long windowStart) {
        try {
            String key = buildKey(windowKey, windowType, windowStart);
            String json = redisTemplate.opsForValue().get(key);
            
            if (json != null) {
                return objectMapper.readValue(json, WindowResult.class);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve cached window result: {}", e.getMessage());
        }
        return null;
    }
    
    public List<WindowResult> getRecent(String pattern, int limit) {
        List<WindowResult> results = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + pattern);
            if (keys != null) {
                for (String key : keys) {
                    if (results.size() >= limit) break;
                    
                    String json = redisTemplate.opsForValue().get(key);
                    if (json != null) {
                        results.add(objectMapper.readValue(json, WindowResult.class));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve recent cached results: {}", e.getMessage());
        }
        return results;
    }
    
    private String buildKey(WindowResult result) {
        return buildKey(result.getWindowKey(), result.getWindowType(), result.getWindowStart());
    }
    
    private String buildKey(String windowKey, String windowType, long windowStart) {
        return String.format("%s%s:%s:%d", KEY_PREFIX, windowKey, windowType, windowStart);
    }
}
