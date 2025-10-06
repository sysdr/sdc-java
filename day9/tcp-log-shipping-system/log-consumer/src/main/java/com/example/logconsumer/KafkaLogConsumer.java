package com.example.logconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class KafkaLogConsumer {

    private final LogRepository logRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter logsProcessedCounter;
    private final Counter logsPersistedCounter;
    private final Counter logsCachedCounter;

    public KafkaLogConsumer(
            LogRepository logRepository,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.logRepository = logRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        this.logsProcessedCounter = Counter.builder("logs_processed_total")
                .description("Total logs processed from Kafka")
                .register(meterRegistry);
        
        this.logsPersistedCounter = Counter.builder("logs_persisted_total")
                .description("Total logs persisted to PostgreSQL")
                .register(meterRegistry);
        
        this.logsCachedCounter = Counter.builder("logs_cached_total")
                .description("Total logs cached in Redis")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeLog(String logJson) {
        try {
            logsProcessedCounter.increment();
            
            // Parse log entry
            LogEntry logEntry = objectMapper.readValue(logJson, LogEntry.class);
            logEntry.setCreatedAt(Instant.now());
            
            // Persist to PostgreSQL
            logRepository.save(logEntry);
            logsPersistedCounter.increment();
            log.debug("Persisted log: {}", logEntry.getId());
            
            // Cache recent ERROR logs in Redis
            if ("ERROR".equals(logEntry.getLevel())) {
                String cacheKey = "recent_errors:" + logEntry.getId();
                redisTemplate.opsForValue().set(cacheKey, logJson, Duration.ofHours(1));
                logsCachedCounter.increment();
                log.debug("Cached error log in Redis: {}", logEntry.getId());
            }
            
        } catch (Exception e) {
            log.error("Error processing log message: {}", logJson, e);
        }
    }
}
