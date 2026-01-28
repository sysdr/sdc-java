package com.example.facetedsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    public FacetedSearchResponse getCachedSearchResult(String cacheKey) {
        try {
            return (FacetedSearchResponse) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to retrieve from cache: {}", cacheKey, e);
            return null;
        }
    }

    public void cacheSearchResult(String cacheKey, FacetedSearchResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache search result: {}", cacheKey, e);
        }
    }

    public void invalidateCache(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern + "*"));
            log.info("Invalidated cache for pattern: {}", pattern);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache: {}", pattern, e);
        }
    }
}
