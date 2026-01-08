package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEvent;
import com.example.logconsumer.repository.LogEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class LogConsumerService {

    private final LogEventRepository logRepository;
    private final LogEnrichmentService enrichmentService;
    private final LogAggregationService aggregationService;
    private final DeadLetterQueueService dlqService;
    
    private final ExecutorService processingPool;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    public LogConsumerService(LogEventRepository logRepository,
                             LogEnrichmentService enrichmentService,
                             LogAggregationService aggregationService,
                             DeadLetterQueueService dlqService,
                             MeterRegistry meterRegistry) {
        this.logRepository = logRepository;
        this.enrichmentService = enrichmentService;
        this.aggregationService = aggregationService;
        this.dlqService = dlqService;
        
        // Thread pool for parallel processing within batches
        this.processingPool = Executors.newFixedThreadPool(10);
        
        // Metrics
        this.successCounter = Counter.builder("log.consumer.success")
                .description("Successfully processed logs")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("log.consumer.failure")
                .description("Failed log processing")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("log.consumer.processing.time")
                .description("Log processing latency")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "application-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeLogs(ConsumerRecords<String, LogEvent> records, Acknowledgment ack) {
        
        log.info("Received batch of {} log events", records.count());
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Process each record in parallel
        for (ConsumerRecord<String, LogEvent> record : records) {
            CompletableFuture<Void> future = CompletableFuture
                    .supplyAsync(() -> {
                        Timer.Sample sample = Timer.start();
                        try {
                            processLogWithRetry(record);
                            sample.stop(processingTimer);
                            successCounter.increment();
                            return null;
                        } catch (Exception e) {
                            failureCounter.increment();
                            handleProcessingError(record, e);
                            return null;
                        }
                    }, processingPool);
            
            futures.add(future);
        }
        
        // Wait for all messages in batch to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Manual offset commit after successful batch processing
        ack.acknowledge();
        
        log.info("Batch processing completed. Offsets committed.");
    }

    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000)
    )
    @Transactional
    public void processLogWithRetry(ConsumerRecord<String, LogEvent> record) {
        LogEvent logEvent = record.value();
        
        // Step 1: Enrich the log event
        LogEvent enriched = enrichmentService.enrich(logEvent);
        
        // Step 2: Persist to PostgreSQL
        logRepository.save(enriched);
        
        // Step 3: Update Redis aggregations
        aggregationService.updateMetrics(enriched);
        
        log.debug("Processed log: id={}, service={}, level={}", 
                enriched.getId(), enriched.getService(), enriched.getLevel());
    }

    @Recover
    public void handleProcessingError(ConsumerRecord<String, LogEvent> record, Exception e) {
        log.error("Failed to process log after retries: id={}", record.value().getId(), e);
        dlqService.sendToDLQ(record, e, 3);
    }
}
