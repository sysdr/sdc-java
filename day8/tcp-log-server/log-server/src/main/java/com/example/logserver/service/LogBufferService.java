package com.example.logserver.service;

import com.example.logserver.metrics.LogMetrics;
import com.example.logserver.model.LogEntry;
import com.example.logserver.repository.LogRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Buffers log messages and writes them to PostgreSQL in batches.
 * 
 * Features:
 * - Bounded in-memory buffer (prevents OOM)
 * - Batch writes for efficiency
 * - Circuit breaker for database failures
 * - Drop-oldest strategy when buffer full
 * - Graceful shutdown with buffer flush
 */
@Service
@Slf4j
public class LogBufferService {

    private final LogRepository logRepository;
    private final LogMetrics logMetrics;
    private final CircuitBreaker circuitBreaker;
    private final BlockingQueue<Map<String, Object>> buffer;
    private final AtomicLong droppedCount = new AtomicLong(0);

    @Value("${buffer.max-size:10000}")
    private int maxBufferSize;

    @Value("${buffer.batch-size:1000}")
    private int batchSize;

    public LogBufferService(
            LogRepository logRepository,
            LogMetrics logMetrics,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.logRepository = logRepository;
        this.logMetrics = logMetrics;
        this.buffer = new LinkedBlockingQueue<>();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("database");
    }

    /**
     * Add log to buffer. Returns false if buffer is full.
     */
    public boolean addLog(Map<String, Object> logData) {
        if (buffer.size() >= maxBufferSize) {
            droppedCount.incrementAndGet();
            logMetrics.recordBufferOverflow();
            return false;
        }
        
        buffer.offer(logData);
        logMetrics.recordBufferSize(buffer.size());
        return true;
    }

    /**
     * Flush buffer to database every 5 seconds or when batch size reached.
     */
    @Scheduled(fixedDelay = 5000)
    public void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }

        List<Map<String, Object>> batch = new ArrayList<>();
        buffer.drainTo(batch, batchSize);

        if (batch.isEmpty()) {
            return;
        }

        try {
            circuitBreaker.executeRunnable(() -> writeBatch(batch));
            logMetrics.recordBatchWritten(batch.size());
            log.debug("Flushed {} logs to database", batch.size());
        } catch (Exception e) {
            log.error("Failed to write batch to database: {}", e.getMessage());
            logMetrics.recordWriteFailure();
            
            // Re-queue logs if possible
            for (Map<String, Object> log : batch) {
                if (!buffer.offer(log)) {
                    droppedCount.incrementAndGet();
                }
            }
        }
    }

    @Transactional
    protected void writeBatch(List<Map<String, Object>> batch) {
        List<LogEntry> entities = batch.stream()
            .map(this::convertToEntity)
            .toList();
        
        logRepository.saveAll(entities);
    }

    private LogEntry convertToEntity(Map<String, Object> logData) {
        Instant timestamp = Instant.parse(logData.get("timestamp").toString());
        String level = logData.get("level").toString();
        String message = logData.get("message").toString();
        String source = logData.getOrDefault("source", "unknown").toString();

        Map<String, Object> metadata = (Map<String, Object>) 
            logData.getOrDefault("metadata", Map.of());

        return LogEntry.builder()
            .timestamp(timestamp)
            .level(level)
            .message(message)
            .source(source)
            .metadata(metadata)
            .receivedAt(Instant.now())
            .build();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Flushing remaining {} logs before shutdown", buffer.size());
        flushBuffer();
        log.info("Dropped {} logs due to buffer overflow during lifetime", 
            droppedCount.get());
    }

    public int getBufferSize() {
        return buffer.size();
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }
}
