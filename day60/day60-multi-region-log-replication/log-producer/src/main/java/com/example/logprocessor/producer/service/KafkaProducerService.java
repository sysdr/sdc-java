package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Core service that publishes LogEvents to Kafka.
 *
 * Metrics exposed:
 *   - log.producer.send.total          (counter, tagged by region + status)
 *   - log.producer.send.duration       (timer)
 *
 * The partitioning key is the correlationId. This guarantees that all events
 * sharing a trace end up in the same partition, preserving intra-trace ordering
 * even across consumer rebalances.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final String region;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer sendTimer;

    public KafkaProducerService(
            KafkaTemplate<String, LogEvent> kafkaTemplate,
            @Value("${app.region}") String region,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.region = region;

        // Micrometer counters — scraped by Prometheus
        this.successCounter = meterRegistry.counter("log.producer.send", "region", region, "status", "success");
        this.failureCounter = meterRegistry.counter("log.producer.send", "region", region, "status", "failure");
        this.sendTimer = meterRegistry.timer("log.producer.send.duration", "region", region);
    }

    /**
     * Publishes a log event to the region-local topic.
     * Uses the correlationId as the Kafka message key for partition affinity.
     *
     * @return ListenableFuture that resolves with the SendResult or completes exceptionally
     */
    public ListenableFuture<SendResult<String, LogEvent>> publish(LogEvent event) {
        String topic = "log-events-" + region;
        String key = event.correlationId() != null ? event.correlationId() : event.eventId();

        Timer.Sample sample = Timer.start();

        ListenableFuture<SendResult<String, LogEvent>> future = kafkaTemplate.send(topic, key, event);

        future.addCallback(new ListenableFutureCallback<SendResult<String, LogEvent>>() {
            @Override
            public void onSuccess(SendResult<String, LogEvent> result) {
                sample.stop(sendTimer);
                successCounter.increment();
                log.debug("Event {} published to {}:{} offset={}",
                        event.eventId(), topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                sample.stop(sendTimer);
                failureCounter.increment();
                log.error("Failed to publish event {}: {}", event.eventId(), ex.getMessage());
            }
        });

        return future;
    }
}
