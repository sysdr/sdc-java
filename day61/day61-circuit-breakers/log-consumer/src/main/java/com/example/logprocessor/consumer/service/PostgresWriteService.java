package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEventEntity;
import com.example.logprocessor.consumer.repository.LogEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Handles persistence of log events to PostgreSQL.
 *
 * Circuit Breaker: "postgresWriter"
 *   - TIME_BASED window, 10s, 60% failure threshold.
 *   - PostgreSQL failures can be transient (connection pool exhaustion,
 *     momentary network lag). The 10s window avoids tripping on brief blips.
 *
 * Fallback: in-memory buffer (max 500 events). The consumer continues
 * processing Kafka messages. A reconciler task flushes the buffer when
 * the breaker closes.
 */
@Service
public class PostgresWriteService {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresWriteService.class);
    private static final int BUFFER_MAX = 500;

    private final LogEventRepository repository;
    private final RedisCacheService cacheService;
    private final List<LogEventEntity> writeBuffer = new CopyOnWriteArrayList<>();

    private final Counter writeSuccessCounter;
    private final Counter writeFallbackCounter;

    public PostgresWriteService(
            LogEventRepository repository,
            RedisCacheService cacheService,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.writeSuccessCounter = Counter.builder("consumer.postgres.write.success.total")
                .description("Successful PostgreSQL writes")
                .register(meterRegistry);
        this.writeFallbackCounter = Counter.builder("consumer.postgres.write.fallback.total")
                .description("Writes routed to in-memory buffer")
                .register(meterRegistry);
    }

    /**
     * Persist a log event: write to PostgreSQL AND cache in Redis.
     * Both operations have independent circuit breakers.
     */
    @CircuitBreaker(name = "postgresWriter", fallbackMethod = "writeFallback")
    @Retry(name = "postgresRetry")
    @Transactional
    public void persist(LogEventEntity entity) {
        repository.save(entity);
        writeSuccessCounter.increment();
        LOG.debug("Persisted event: {}", entity.getEventId());

        // Cache write is independent — if Redis is down, this is a no-op (its own fallback).
        cacheService.put(entity);
    }

    /**
     * Fallback: buffer the event in memory.
     * Eviction strategy: drop oldest if full (same as the producer DLQ).
     */
    private void writeFallback(LogEventEntity entity, Throwable ex) {
        LOG.warn("[POSTGRES-CB] Write skipped for {}. Reason: {}. Buffering.",
                entity.getEventId(), ex.getClass().getSimpleName());
        writeFallbackCounter.increment();

        if (writeBuffer.size() >= BUFFER_MAX) {
            writeBuffer.remove(0);
            LOG.warn("Write buffer full. Dropped oldest event.");
        }
        writeBuffer.add(entity);
    }

    /** Flush buffered events back to PostgreSQL (called by reconciler). */
    public List<LogEventEntity> drainWriteBuffer() {
        List<LogEventEntity> drained = new CopyOnWriteArrayList<>(writeBuffer);
        writeBuffer.clear();
        return drained;
    }

    public int getWriteBufferSize() {
        return writeBuffer.size();
    }
}
