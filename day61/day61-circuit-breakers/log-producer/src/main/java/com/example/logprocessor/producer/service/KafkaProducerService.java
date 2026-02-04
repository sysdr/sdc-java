package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.DeadLetterEvent;
import com.example.logprocessor.producer.model.LogEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central Kafka produce path with full Resilience4j decoration.
 *
 * Circuit Breaker: "kafkaBroker"
 *   - Trips when Kafka acks are failing (time-based window).
 *   - On trip → event goes to in-memory dead-letter buffer.
 *
 * Bulkhead: "kafkaBulkhead"
 *   - Limits concurrent produce calls to 30.
 *   - Prevents thread exhaustion if Kafka is slow but not fully down.
 *
 * Retry: "kafkaRetry"
 *   - 2 retries with 100ms exponential backoff.
 *   - INSIDE the circuit breaker — retries are invisible to the breaker.
 */
@Service
public class KafkaProducerService {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "log-events";
    private static final int DLQ_MAX_SIZE = 1000;  // Hard cap on dead-letter buffer

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final List<DeadLetterEvent> deadLetterBuffer = new CopyOnWriteArrayList<>();
    private final AtomicInteger deadLetterDrops = new AtomicInteger(0);

    // Metrics
    private final Counter produceSuccessCounter;
    private final Counter produceFallbackCounter;
    private final Counter deadLetterDropCounter;
    private final Gauge deadLetterBufferGauge;

    public KafkaProducerService(
            KafkaTemplate<String, LogEvent> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;

        this.produceSuccessCounter = Counter.builder("producer.kafka.produce.success.total")
                .description("Successfully produced messages to Kafka")
                .register(meterRegistry);
        this.produceFallbackCounter = Counter.builder("producer.kafka.produce.fallback.total")
                .description("Messages routed to dead-letter buffer (breaker tripped)")
                .register(meterRegistry);
        this.deadLetterDropCounter = Counter.builder("producer.kafka.deadletter.dropped.total")
                .description("Messages dropped from dead-letter buffer (buffer full)")
                .register(meterRegistry);
        this.deadLetterBufferGauge = Gauge.builder("producer.kafka.deadletter.buffer.size", this, 
                svc -> (double) svc.deadLetterBuffer.size())
                .description("Current number of events in the dead-letter buffer")
                .register(meterRegistry);
    }

    /**
     * Primary produce path.
     *
     * @param event the log event to publish
     * @return CompletableFuture of the Kafka SendResult
     */
    @CircuitBreaker(name = "kafkaBroker", fallbackMethod = "produceFallback")
    @Bulkhead(name = "kafkaBulkhead")
    @Retry(name = "kafkaRetry")
    public CompletableFuture<SendResult<String, LogEvent>> produce(LogEvent event) {
        LOG.debug("[{}] Producing to topic '{}': eventId={}", event.correlationId(), TOPIC, event.eventId());

        // Partition key = source, ensures events from the same source land on the same partition
        // This preserves ordering per source — critical for log correlation.
        CompletableFuture<SendResult<String, LogEvent>> future =
                kafkaTemplate.send(TOPIC, event.source(), event);

        future.thenAccept(result -> {
            produceSuccessCounter.increment();
            LOG.debug("[{}] Produced to partition={}, offset={}",
                    event.correlationId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });

        return future;
    }

    /**
     * Fallback — invoked when:
     *   1. Circuit breaker is OPEN (Kafka broker detected as unhealthy), OR
     *   2. All retries are exhausted (Kafka is slow but not fully down)
     *
     * Writes to an in-memory buffer. If the buffer is full, we drop the OLDEST
     * entry (not the newest) — old log data is less valuable than fresh data.
     *
     * Production extension: flush this buffer to SQS/SNS on a background timer.
     */
    private CompletableFuture<SendResult<String, LogEvent>> produceFallback(LogEvent event, Throwable ex) {
        LOG.warn("[CIRCUIT-BREAKER] Kafka unreachable for event {}. Reason: {}. Buffering locally.",
                event.eventId(), ex.getClass().getSimpleName());

        produceFallbackCounter.increment();

        DeadLetterEvent dle = new DeadLetterEvent(event, ex.getMessage(), 1, Instant.now());

        if (deadLetterBuffer.size() >= DLQ_MAX_SIZE) {
            // Evict oldest
            deadLetterBuffer.remove(0);
            deadLetterDrops.incrementAndGet();
            deadLetterDropCounter.increment();
            LOG.warn("Dead-letter buffer full. Dropped oldest entry. Total drops: {}", deadLetterDrops.get());
        }

        deadLetterBuffer.add(dle);
        return CompletableFuture.completedFuture(null);  // Caller sees success — we accepted the event.
    }

    /**
     * Drain the dead-letter buffer back to Kafka (called by a scheduled task
     * or manually when the breaker closes). Returns events that were successfully re-produced.
     */
    public List<DeadLetterEvent> drainDeadLetterBuffer() {
        List<DeadLetterEvent> drained = new CopyOnWriteArrayList<>(deadLetterBuffer);
        deadLetterBuffer.clear();
        LOG.info("Drained {} events from dead-letter buffer for re-production.", drained.size());
        return drained;
    }

    /** Expose buffer size for health checks. */
    public int getDeadLetterBufferSize() {
        return deadLetterBuffer.size();
    }
}
