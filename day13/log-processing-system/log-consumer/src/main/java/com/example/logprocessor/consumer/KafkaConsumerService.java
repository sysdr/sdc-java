package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    private final LogEventRepository repository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    private final Counter errorCounter;

    public KafkaConsumerService(
            LogEventRepository repository,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processedCounter = Counter.builder("log.consumer.processed")
            .description("Successfully processed log events")
            .register(meterRegistry);
        this.errorCounter = Counter.builder("log.consumer.error")
            .description("Failed to process log events")
            .register(meterRegistry);
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    public void consumeLogEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            String id = node.get("id").asText();
            String level = node.get("level").asText();
            String logMessage = node.get("message").asText();
            String source = node.get("source").asText();
            Instant timestamp = Instant.parse(node.get("timestamp").asText());
            String metadata = node.get("metadata").toString();

            // Cache recent error logs in Redis
            if ("ERROR".equals(level) || "WARN".equals(level)) {
                String cacheKey = "recent:" + level + ":" + id;
                redisTemplate.opsForValue().set(cacheKey, message, Duration.ofHours(1));
            }

            // Persist to PostgreSQL
            LogEventEntity entity = new LogEventEntity(
                id, level, logMessage, source, timestamp, metadata);
            repository.save(entity);

            processedCounter.increment();
            logger.debug("Processed log event: {} from source: {}", id, source);

        } catch (Exception e) {
            errorCounter.increment();
            logger.error("Error processing log event", e);
        }
    }
}
