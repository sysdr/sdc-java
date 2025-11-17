package com.example.logprocessor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class KafkaConsumerService {
    
    private final LogRepository logRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Timer processingTimer;
    private final Counter processedCounter;
    private final Counter errorCounter;

    public KafkaConsumerService(LogRepository logRepository,
                                RedisTemplate<String, String> redisTemplate,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.logRepository = logRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.processingTimer = Timer.builder("log.processing.time")
                .description("Time taken to process log message")
                .publishPercentiles(0.5, 0.95, 0.99, 0.999)
                .register(meterRegistry);
        this.processedCounter = Counter.builder("log.processed.count")
                .description("Number of logs processed")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("log.processing.errors")
                .description("Number of processing errors")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group", concurrency = "3")
    public void consumeLog(@Payload String message,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        
        processingTimer.record(() -> {
            try {
                log.debug("Received log from partition {} offset {}", partition, offset);
                
                // Deserialize
                LogEventDto dto = objectMapper.readValue(message, LogEventDto.class);
                
                // Check cache for duplicate detection
                String cacheKey = "log:" + dto.getId();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                    log.debug("Duplicate log detected: {}", dto.getId());
                    return;
                }
                
                // Process and persist
                LogEntity entity = LogEntity.builder()
                        .id(dto.getId())
                        .level(dto.getLevel())
                        .message(dto.getMessage())
                        .source(dto.getSource())
                        .timestamp(dto.getTimestamp())
                        .traceId(dto.getTraceId())
                        .processedAt(Instant.now())
                        .build();
                
                logRepository.save(entity);
                
                // Cache for 1 hour to detect duplicates
                redisTemplate.opsForValue().set(cacheKey, "1", Duration.ofHours(1));
                
                processedCounter.increment();
                
                log.debug("Successfully processed log: {}", dto.getId());
                
            } catch (Exception e) {
                errorCounter.increment();
                log.error("Error processing log message from partition {} offset {}", 
                        partition, offset, e);
            }
        });
    }
}
