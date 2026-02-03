package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.example.logprocessor.consumer.model.PersistedLogEvent;
import com.example.logprocessor.consumer.repository.LogEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Main Kafka consumer.
 *
 * Listens on BOTH topics:
 *   - log-events-{localRegion}          (local writes)
 *   - {remoteRegion}.log-events-*       (MirrorMaker-replicated from remote region)
 *
 * Processing pipeline per event:
 *   1. Deduplication check (bloom filter + Redis)
 *   2. Reorder buffer insertion
 *   3. Periodic flush from reorder buffer
 *   4. Persist flushed events to PostgreSQL
 *
 * The @KafkaListener uses SpEL to dynamically construct topic names from
 * the application properties, keeping the code region-agnostic.
 */
@Service
public class LogConsumerService {

    private static final Logger log = LoggerFactory.getLogger(LogConsumerService.class);

    private final DeduplicationService deduplicationService;
    private final ReorderBufferService reorderBuffer;
    private final LogEventRepository repository;
    private final String region;

    private final Counter processedCounter;
    private final Counter duplicateCounter;
    private final Counter lateCounter;

    public LogConsumerService(
            DeduplicationService deduplicationService,
            ReorderBufferService reorderBuffer,
            @Lazy LogEventRepository repository,
            @Value("${app.region}") String region,
            MeterRegistry meterRegistry
    ) {
        this.deduplicationService = deduplicationService;
        this.reorderBuffer = reorderBuffer;
        this.repository = repository;
        this.region = region;

        this.processedCounter = meterRegistry.counter("log.consumer.processed", "region", region);
        this.duplicateCounter = meterRegistry.counter("log.consumer.duplicates", "region", region);
        this.lateCounter = meterRegistry.counter("log.consumer.late", "region", region);
    }

    /**
     * Consumes events from the local region topic.
     * Topic name is resolved at startup from the 'app.region' property.
     */
    @KafkaListener(
            topics = "${app.consumer.local-topic}",
            groupId = "${app.consumer.group-id}",
            concurrency = "4"
    )
    public void consumeLocalEvents(LogEvent event, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        processEvent(event, partition);
    }

    /**
     * Consumes MirrorMaker-replicated events from the remote region.
     * MirrorMaker prefixes replicated topics with the source cluster alias.
     */
    @KafkaListener(
            topics = "${app.consumer.remote-topic}",
            groupId = "${app.consumer.group-id}",
            concurrency = "2"  // Lower concurrency for replicated stream — it's a secondary flow
    )
    public void consumeReplicatedEvents(LogEvent event, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        processEvent(event, partition);
    }

    /**
     * Core processing pipeline.
     */
    private void processEvent(LogEvent event, int partition) {
        log.debug("Processing event {} from partition {} sourceRegion={}", event.eventId(), partition, event.sourceRegion());

        // Step 1: Deduplication
        if (deduplicationService.isDuplicate(event.eventId())) {
            duplicateCounter.increment();
            log.debug("Duplicate event skipped: {}", event.eventId());
            return;
        }

        // Step 2: Reorder buffer
        boolean buffered = reorderBuffer.add(event);
        if (!buffered) {
            // Late arrival — persist directly with a flag for reconciliation
            lateCounter.increment();
            persistEvent(event);
            log.warn("Late event persisted directly: {}", event.eventId());
            return;
        }

        // Step 3: Flush ready events
        List<LogEvent> readyEvents = reorderBuffer.flush();
        for (LogEvent ready : readyEvents) {
            persistEvent(ready);
            processedCounter.increment();
        }
    }

    @Transactional
    private void persistEvent(LogEvent event) {
        PersistedLogEvent entity = PersistedLogEvent.from(event, region);
        repository.saveOrIgnore(entity);
    }
}
