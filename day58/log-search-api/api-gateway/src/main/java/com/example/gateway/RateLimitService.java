package com.example.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    private static final long STANDARD_QUOTA = 1000; // requests per hour
    private static final long PREMIUM_QUOTA = 10000;
    private static final Duration WINDOW = Duration.ofHours(1);

    public Mono<Boolean> checkQuota(String apiKey, int cost) {
        String key = "rate_limit:" + apiKey;
        long quota = getQuotaForKey(apiKey);

        return redisTemplate.opsForValue()
                .increment(key, cost)
                .flatMap(currentUsage -> {
                    if (currentUsage == cost) {
                        // First request in window, set expiration
                        return redisTemplate.expire(key, WINDOW)
                                .thenReturn(true);
                    }
                    
                    if (currentUsage > quota) {
                        log.warn("Rate limit exceeded for key: {} ({}/{})", 
                                apiKey, currentUsage, quota);
                        return Mono.just(false);
                    }
                    
                    return Mono.just(true);
                })
                .onErrorResume(e -> {
                    log.error("Rate limit check failed, failing open", e);
                    return Mono.just(true); // Fail open to prevent total outage
                });
    }

    private long getQuotaForKey(String apiKey) {
        // In production, lookup from database
        // For demo, use key prefix to determine tier
        return apiKey.startsWith("premium_") ? PREMIUM_QUOTA : STANDARD_QUOTA;
    }
}
