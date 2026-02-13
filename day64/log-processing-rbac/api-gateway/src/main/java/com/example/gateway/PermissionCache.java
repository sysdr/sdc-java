package com.example.gateway;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PermissionCache {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    public PermissionCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean checkPermission(Long userId, String resource) {
        String key = "perm:" + userId + ":" + resource;
        String cached = redisTemplate.opsForValue().get(key);
        
        if ("true".equals(cached)) {
            return true;
        } else if ("false".equals(cached)) {
            return false;
        }
        
        // Cache miss - caller should check database and cache result
        return false;
    }

    public void cachePermission(Long userId, String resource, boolean allowed) {
        String key = "perm:" + userId + ":" + resource;
        redisTemplate.opsForValue().set(key, String.valueOf(allowed), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void invalidateUserPermissions(Long userId) {
        // In production, use Redis SCAN to find all keys for user
        // For now, just log
        System.out.println("Invalidating permissions for user: " + userId);
    }
}
