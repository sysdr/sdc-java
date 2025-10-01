package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.example.logprocessor.consumer.repository.LogEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LogStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LogStorageService.class);
    private static final String REDIS_LOG_PREFIX = "log:hot:";
    private static final Duration HOT_STORAGE_TTL = Duration.ofHours(24);

    private final LogEventRepository logEventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LogStorageService(LogEventRepository logEventRepository,
                           RedisTemplate<String, String> redisTemplate,
                           FileStorageService fileStorageService) {
        this.logEventRepository = logEventRepository;
        this.redisTemplate = redisTemplate;
        this.fileStorageService = fileStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Transactional
    public void storeLogEvent(LogEvent logEvent) {
        try {
            // Store in warm storage (PostgreSQL) for complex queries
            LogEvent savedEvent = logEventRepository.save(logEvent);
            logger.debug("Stored log event in warm storage: id={}", savedEvent.getId());

            // Store in hot storage (Redis) for fast access to recent logs
            String redisKey = REDIS_LOG_PREFIX + logEvent.getTraceId();
            String jsonValue = objectMapper.writeValueAsString(logEvent);
            redisTemplate.opsForValue().set(redisKey, jsonValue, HOT_STORAGE_TTL);
            logger.debug("Stored log event in hot storage: trace_id={}", logEvent.getTraceId());

            // Conditionally store in cold storage based on level and retention policies
            if (shouldStoreToColdStorage(logEvent)) {
                fileStorageService.storeLogEvent(logEvent);
                logger.debug("Stored log event in cold storage: trace_id={}", logEvent.getTraceId());
            }

        } catch (Exception e) {
            logger.error("Failed to store log event: trace_id={}", logEvent.getTraceId(), e);
            throw new RuntimeException("Failed to store log event", e);
        }
    }

    private boolean shouldStoreToColdStorage(LogEvent logEvent) {
        // Store critical logs (ERROR, WARN) and older logs in cold storage
        return "ERROR".equals(logEvent.getLevel()) || 
               "WARN".equals(logEvent.getLevel()) ||
               logEvent.getTimestamp().isBefore(LocalDateTime.now().minusDays(7));
    }
}
