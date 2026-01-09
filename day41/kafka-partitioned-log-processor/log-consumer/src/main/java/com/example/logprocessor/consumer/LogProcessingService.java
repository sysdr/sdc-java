package com.example.logprocessor.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProcessingService {

    private final ProcessedLogRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void processLogEvent(LogEvent event, int partition) {
        // Check for duplicate using Redis cache
        String cacheKey = "processed:" + event.getEventId();
        Boolean exists = redisTemplate.hasKey(cacheKey);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // Simulate processing logic
        ProcessedLog processedLog = ProcessedLog.builder()
                .eventId(event.getEventId())
                .source(event.getSource())
                .level(event.getLevel())
                .message(event.getMessage())
                .application(event.getApplication())
                .hostname(event.getHostname())
                .timestamp(event.getTimestamp())
                .traceId(event.getTraceId())
                .partition(partition)
                .build();

        repository.save(processedLog);

        // Mark as processed in cache (TTL 1 hour)
        redisTemplate.opsForValue().set(cacheKey, "true", Duration.ofHours(1));

        log.info("Processed event {} from source {} on partition {}", 
                event.getEventId(), event.getSource(), partition);
    }
}
