package com.example.consumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class BackpressureKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(BackpressureKafkaConsumer.class);

    private final LogProcessingService processingService;
    private final CircuitBreaker databaseCircuitBreaker;
    private final AtomicInteger activeProcessing = new AtomicInteger(0);
    private static final int MAX_CONCURRENT = 8;

    public BackpressureKafkaConsumer(LogProcessingService processingService,
                                    CircuitBreaker databaseCircuitBreaker) {
        this.processingService = processingService;
        this.databaseCircuitBreaker = databaseCircuitBreaker;
    }

    @KafkaListener(
        topics = "log-events",
        groupId = "log-consumer-group",
        concurrency = "3",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecords<String, LogEvent> records, Acknowledgment ack) {
        if (databaseCircuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker OPEN - pausing consumption. Records in batch: {}", 
                records.count());
            // Don't acknowledge - messages will be redelivered after consumer rebalance
            return;
        }

        log.info("Processing batch of {} records. Active: {}", 
            records.count(), activeProcessing.get());

        Flux.fromIterable(records)
            .flatMap(record -> processRecord(record), MAX_CONCURRENT)
            .doOnNext(result -> activeProcessing.decrementAndGet())
            .doOnError(e -> log.error("Batch processing error", e))
            .onErrorResume(e -> Flux.empty())
            .blockLast();  // Block until all records processed

        ack.acknowledge();
        log.debug("Batch acknowledged");
    }

    private reactor.core.publisher.Mono<String> processRecord(ConsumerRecord<String, LogEvent> record) {
        activeProcessing.incrementAndGet();
        
        return reactor.core.publisher.Mono.fromCallable(() -> {
                LogEvent event = record.value();
                log.debug("Processing log: {}", event.getCorrelationId());
                return event;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(processingService::processLog)
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(e -> !(e instanceof org.springframework.dao.DataAccessException)))
            .doOnError(e -> log.error("Failed to process record after retries: {}", 
                record.value().getCorrelationId(), e))
            .onErrorResume(e -> reactor.core.publisher.Mono.just("FAILED"));
    }
}
