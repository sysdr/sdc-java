package com.example.logprocessor.parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static final String PARSER_STATS_KEY = "parser:stats:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void incrementParseCounter(String logFormat) {
        try {
            String key = PARSER_STATS_KEY + logFormat;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofHours(24));
        } catch (Exception e) {
            logger.warn("Failed to update parse counter in cache", e);
        }
    }
    
    public Long getParseCount(String logFormat) {
        try {
            String key = PARSER_STATS_KEY + logFormat;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.valueOf(value.toString()) : 0L;
        } catch (Exception e) {
            logger.warn("Failed to retrieve parse count from cache", e);
            return 0L;
        }
    }
    
    public void cacheIpLocation(String ip, String location) {
        try {
            String key = "ip:location:" + ip;
            redisTemplate.opsForValue().set(key, location, Duration.ofDays(7));
        } catch (Exception e) {
            logger.warn("Failed to cache IP location", e);
        }
    }
    
    public String getIpLocation(String ip) {
        try {
            String key = "ip:location:" + ip;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.warn("Failed to retrieve IP location from cache", e);
            return null;
        }
    }
}
