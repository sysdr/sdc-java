package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEventEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka consumer listener.
 * Transforms the raw Kafka message into a JPA entity and hands it to
 * PostgresWriteService (which handles its own circuit breakers internally).
 *
 * Error handling: if persist() fails and the fallback buffer is also full,
 * we log and increment a metric. The message is NOT retried by Kafka —
 * we accept the loss for that event to keep the consumer partition moving.
 * This is a deliberate availability-over-consistency trade-off.
 */
@Service
public class LogEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(LogEventConsumer.class);

    private final PostgresWriteService postgresWriteService;
    private final Counter consumedCounter;
    private final Counter errorCounter;

    public LogEventConsumer(PostgresWriteService postgresWriteService, MeterRegistry meterRegistry) {
        this.postgresWriteService = postgresWriteService;
        this.consumedCounter = Counter.builder("consumer.kafka.consumed.total")
                .description("Total messages consumed from Kafka")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("consumer.kafka.consume.errors.total")
                .description("Total consume errors (after all retries)")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    public void onMessage(Map<String, Object> payload) {
        consumedCounter.increment();

        try {
            LogEventEntity entity = mapToEntity(payload);
            LOG.debug("Consumed event: {}", entity.getEventId());
            postgresWriteService.persist(entity);
        } catch (Exception ex) {
            errorCounter.increment();
            LOG.error("Failed to process message: {}", ex.getMessage(), ex);
            // In production: route to a Kafka dead-letter topic here.
        }
    }

    @SuppressWarnings("unchecked")
    private LogEventEntity mapToEntity(Map<String, Object> payload) {
        LogEventEntity entity = new LogEventEntity();
        entity.setEventId((String) payload.get("eventId"));
        entity.setSource((String) payload.get("source"));
        entity.setLevel((String) payload.get("level"));
        entity.setMessage((String) payload.get("message"));
        entity.setCorrelationId((String) payload.get("correlationId"));

        // Timestamp can arrive as String or Long depending on serializer
        Object ts = payload.get("timestamp");
        if (ts instanceof String) {
            entity.setTimestamp(java.time.Instant.parse((String) ts));
        } else if (ts instanceof Long) {
            entity.setTimestamp(java.time.Instant.ofEpochMilli((Long) ts));
        }

        return entity;
    }
}
