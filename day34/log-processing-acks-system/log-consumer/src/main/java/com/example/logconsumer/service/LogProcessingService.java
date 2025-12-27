package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEvent;
import com.example.logconsumer.model.IdempotencyKey;
import com.example.logconsumer.repository.IdempotencyKeyRepository;
import com.example.logconsumer.repository.LogEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogProcessingService {

    private final LogEventRepository logRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DeadLetterQueueService dlqService;

    @Transactional
    @Retryable(
        retryFor = {TransientProcessingException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 10000)
    )
    @CircuitBreaker(name = "logProcessing", fallbackMethod = "fallbackProcess")
    public void processLog(LogEvent event) {
        log.info("Processing log event: id={}, level={}", event.getId(), event.getLevel());

        String redisKey = "idempotency:" + event.getId();
        Boolean isProcessed = redisTemplate.opsForValue().setIfAbsent(
            redisKey, "1", Duration.ofHours(1)
        );

        if (Boolean.FALSE.equals(isProcessed)) {
            log.info("Duplicate detected (Redis): {}", event.getId());
            return;
        }

        if (idempotencyRepository.existsById(event.getId())) {
            log.info("Duplicate detected (DB): {}", event.getId());
            return;
        }

        if (event.isSimulateFailure() && Math.random() < 0.3) {
            log.warn("Simulating transient failure for: {}", event.getId());
            throw new TransientProcessingException("Simulated transient error");
        }

        event.setProcessedAt(Instant.now());
        logRepository.save(event);

        idempotencyRepository.save(IdempotencyKey.builder()
            .messageId(event.getId())
            .processedAt(Instant.now())
            .consumerGroup("log-consumer-group")
            .build());

        log.info("Successfully processed log: {}", event.getId());
    }

    @Recover
    public void recover(Exception e, LogEvent event) {
        log.error("All retry attempts exhausted for message: {}", event.getId(), e);
        dlqService.sendToDeadLetterQueue(event, e);
    }

    public void fallbackProcess(LogEvent event, Exception e) {
        log.error("Circuit breaker opened, sending to DLQ: {}", event.getId(), e);
        dlqService.sendToDeadLetterQueue(event, e);
    }

    public static class TransientProcessingException extends RuntimeException {
        public TransientProcessingException(String message) {
            super(message);
        }
    }
}
