package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KafkaConsumerService {
    
    private final LogRepository logRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesProcessed;
    private final Counter duplicatesSkipped;
    
    public KafkaConsumerService(LogRepository logRepository,
                               RedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.logRepository = logRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagesProcessed = Counter.builder("kafka.messages.processed")
            .description("Total messages processed from Kafka")
            .register(meterRegistry);
        this.duplicatesSkipped = Counter.builder("kafka.duplicates.skipped")
            .description("Duplicate messages skipped")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "logs.udp.ingress", groupId = "log-consumer-group")
    public void consumeLog(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventId = node.get("id").asText();
            
            // Check Redis cache for duplicates
            String cacheKey = "log:processed:" + eventId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                duplicatesSkipped.increment();
                log.debug("Skipping duplicate log: {}", eventId);
                return;
            }
            
            // Check database for duplicates
            if (logRepository.existsByEventId(eventId)) {
                duplicatesSkipped.increment();
                redisTemplate.opsForValue().set(cacheKey, "1", 1, TimeUnit.HOURS);
                log.debug("Skipping duplicate log (found in DB): {}", eventId);
                return;
            }
            
            LogEntity entity = LogEntity.builder()
                .eventId(eventId)
                .source(node.get("source").asText())
                .level(node.get("level").asText())
                .message(node.get("message").asText())
                .timestamp(Instant.parse(node.get("timestamp").asText()))
                .sequenceNumber(node.get("sequenceNumber").asLong())
                .build();
            
            logRepository.save(entity);
            redisTemplate.opsForValue().set(cacheKey, "1", 1, TimeUnit.HOURS);
            messagesProcessed.increment();
            
            log.info("Processed log: id={} seq={}", eventId, entity.getSequenceNumber());
            
        } catch (Exception e) {
            log.error("Error processing message from Kafka", e);
        }
    }
}
