package com.example.logprocessor.gateway.config;

import com.example.logprocessor.gateway.model.LogEventPayload;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound client to log-producer.
 *
 * Composition order (outermost → innermost):
 *   Bulkhead → CircuitBreaker → Retry → actual HTTP call
 *
 * This ordering is CRITICAL:
 *   - Bulkhead limits concurrency BEFORE we even check the breaker state.
 *   - CircuitBreaker sees all failures (including retried ones).
 *   - Retry is innermost so exhausted retries count as ONE failure to the breaker.
 */
@Service
public class ProducerClient {
    private static final Logger LOG = LoggerFactory.getLogger(ProducerClient.class);

    private final RestTemplate restTemplate;
    private final String producerUrl;
    private final Counter fallbackCounter;

    public ProducerClient(
            RestTemplateBuilder builder,
            @Value("${gateway.producer-service.url:http://log-producer:8081}") String producerUrl,
            MeterRegistry meterRegistry
    ) {
        this.restTemplate = builder.build();
        this.producerUrl = producerUrl;
        this.fallbackCounter = Counter.builder("gateway.producer.fallback.total")
                .description("Total times the producer fallback was invoked")
                .register(meterRegistry);
    }

    /**
     * Primary path: forward event to log-producer via HTTP POST.
     *
     * @throws Exception propagated to circuit breaker for failure accounting
     */
    @CircuitBreaker(name = "producerService", fallbackMethod = "produceFallback")
    @Bulkhead(name = "producerBulkhead")
    @Retry(name = "producerRetry")
    public ResponseEntity<Void> produce(LogEventPayload payload) {
        String endpoint = producerUrl + "/api/logs/internal/produce";
        LOG.debug("[{}] Forwarding event to log-producer: {}", payload.correlationId(), payload.eventId());
        return restTemplate.postForEntity(endpoint, payload, Void.class);
    }

    /**
     * Fallback: invoked when the circuit breaker is OPEN or all retries are exhausted.
     *
     * We do NOT throw — we accept the event into a local dead-letter buffer
     * and let the caller return 202 Accepted. This keeps the gateway available.
     *
     * In production, this buffer would be flushed to a durable queue (SQS, etc.)
     * on a background thread. Here we log it and increment a metric.
     */
    private ResponseEntity<Void> produceFallback(LogEventPayload payload, Throwable ex) {
        LOG.warn("[CIRCUIT-BREAKER-FALLBACK] Producer unreachable for event {}. Reason: {}",
                payload.eventId(), ex.getClass().getSimpleName());
        fallbackCounter.increment();

        // In a real system: write to local WAL or SQS dead-letter queue here.
        // For this lesson we emit the metric and return success to the caller.
        return ResponseEntity.accepted().build();
    }
}
