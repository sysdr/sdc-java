package com.example.logshipper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class LogBatchingService implements ApplicationListener<ContextClosedEvent> {
    
    private final BlockingQueue<LogEvent> buffer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicInteger currentBatchSize = new AtomicInteger(0);
    
    private final Counter logsReceived;
    private final Counter logsSent;
    private final Counter logsDropped;
    private final Counter batchesSent;
    private final Timer batchFlushTimer;
    
    @Value("${batching.max-batch-size}")
    private int maxBatchSize;
    
    @Value("${batching.buffer-capacity}")
    private int bufferCapacity;
    
    @Value("${kafka.topic}")
    private String kafkaTopic;
    
    public LogBatchingService(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper,
                             MeterRegistry registry,
                             @Value("${batching.buffer-capacity}") int bufferCapacity) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.buffer = new ArrayBlockingQueue<>(bufferCapacity);
        
        // Metrics
        this.logsReceived = Counter.builder("logs.received")
            .description("Number of logs received")
            .register(registry);
        this.logsSent = Counter.builder("logs.sent")
            .description("Number of logs sent to Kafka")
            .register(registry);
        this.logsDropped = Counter.builder("logs.dropped")
            .description("Number of logs dropped due to buffer full")
            .register(registry);
        this.batchesSent = Counter.builder("batches.sent")
            .description("Number of batches sent")
            .register(registry);
        this.batchFlushTimer = Timer.builder("batch.flush.duration")
            .description("Time taken to flush batch")
            .register(registry);
        
        Gauge.builder("buffer.size", buffer, BlockingQueue::size)
            .description("Current buffer size")
            .register(registry);
        Gauge.builder("buffer.remaining.capacity", buffer, BlockingQueue::remainingCapacity)
            .description("Remaining buffer capacity")
            .register(registry);
    }
    
    /**
     * Add log to batch buffer with backpressure handling
     */
    public void addLog(LogEvent event) {
        logsReceived.increment();
        
        boolean added = buffer.offer(event);
        if (!added) {
            logsDropped.increment();
            log.warn("Buffer full, dropping log: {}", event.getId());
            return;
        }
        
        currentBatchSize.incrementAndGet();
        
        // Size-based trigger: flush when batch is full
        if (currentBatchSize.get() >= maxBatchSize) {
            flushBatch();
        }
    }
    
    /**
     * Time-based trigger: flush every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduledFlush() {
        if (currentBatchSize.get() > 0) {
            flushBatch();
        }
    }
    
    /**
     * Flush current batch to Kafka
     */
    private synchronized void flushBatch() {
        if (currentBatchSize.get() == 0) {
            return;
        }
        
        batchFlushTimer.record(() -> {
            List<LogEvent> batch = new ArrayList<>();
            buffer.drainTo(batch, maxBatchSize);
            
            if (batch.isEmpty()) {
                return;
            }
            
            try {
                String batchJson = objectMapper.writeValueAsString(batch);
                kafkaTemplate.send(kafkaTopic, batchJson).get();
                
                logsSent.increment(batch.size());
                batchesSent.increment();
                currentBatchSize.addAndGet(-batch.size());
                
                log.info("Flushed batch of {} logs to Kafka", batch.size());
                
            } catch (Exception e) {
                log.error("Failed to send batch to Kafka", e);
                // Re-add to buffer if send fails (circuit breaker pattern)
                batch.forEach(event -> {
                    if (!buffer.offer(event)) {
                        logsDropped.increment();
                    }
                });
            }
        });
    }
    
    /**
     * Graceful shutdown: flush remaining logs
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Shutting down, flushing remaining {} logs", currentBatchSize.get());
        flushBatch();
    }
}
