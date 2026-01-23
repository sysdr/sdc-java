package com.example.sessionization.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionCacheService {
    private static final Logger log = LoggerFactory.getLogger(SessionCacheService.class);
    private static final Duration SESSION_TTL = Duration.ofMinutes(35); // Slightly longer than session gap
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionCacheService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheSession(String userId, SessionAggregate session) {
        try {
            String key = "session:" + userId;
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL);
        } catch (Exception e) {
            log.error("Error caching session for user {}", userId, e);
        }
    }

    public SessionAggregate getSession(String userId) {
        try {
            String key = "session:" + userId;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, SessionAggregate.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving session for user {}", userId, e);
        }
        return null;
    }
}
