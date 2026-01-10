package com.example.producer.service;

import com.example.producer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TransactionalProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter transactionAbortCounter;
    private final Timer sendTimer;

    public TransactionalProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate,
                                       MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.successCounter = Counter.builder("log_producer.send.success")
                .description("Successfully sent messages")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("log_producer.send.failure")
                .description("Failed message sends")
                .register(meterRegistry);
        this.transactionAbortCounter = Counter.builder("log_producer.transaction.abort")
                .description("Aborted transactions")
                .register(meterRegistry);
        this.sendTimer = Timer.builder("log_producer.send.duration")
                .description("Message send duration")
                .register(meterRegistry);
    }

    /**
     * Send log event with exactly-once guarantees using Kafka transactions
     */
    public CompletableFuture<SendResult<String, LogEvent>> sendLogEvent(LogEvent event) {
        return sendTimer.record(() -> {
            // Ensure event has ID and timestamp
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            if (event.getTraceId() == null) {
                event.setTraceId(UUID.randomUUID().toString());
            }

            CompletableFuture<SendResult<String, LogEvent>> future = new CompletableFuture<>();

            // Execute in transaction for exactly-once semantics
            kafkaTemplate.executeInTransaction(operations -> {
                try {
                    // Send to primary topic
                    CompletableFuture<SendResult<String, LogEvent>> primarySend = 
                        operations.send("raw-logs", event.getEventId(), event);

                    // Send to audit topic (atomic with primary)
                    operations.send("audit-logs", event.getEventId(), event);

                    primarySend.whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send event {}: {}", event.getEventId(), ex.getMessage());
                            failureCounter.increment();
                            future.completeExceptionally(ex);
                        } else {
                            log.debug("Successfully sent event {} to partition {}", 
                                    event.getEventId(), 
                                    result.getRecordMetadata().partition());
                            successCounter.increment();
                            future.complete(result);
                        }
                    });

                    return null;
                } catch (Exception e) {
                    log.error("Transaction failed for event {}: {}", event.getEventId(), e.getMessage());
                    transactionAbortCounter.increment();
                    future.completeExceptionally(e);
                    throw e;
                }
            });

            return future;
        });
    }

    /**
     * Send batch of events atomically
     */
    public void sendBatchAtomic(java.util.List<LogEvent> events) {
        kafkaTemplate.executeInTransaction(operations -> {
            try {
                for (LogEvent event : events) {
                    if (event.getEventId() == null) {
                        event.setEventId(UUID.randomUUID().toString());
                    }
                    operations.send("raw-logs", event.getEventId(), event);
                }
                log.info("Successfully sent batch of {} events", events.size());
                return null;
            } catch (Exception e) {
                log.error("Batch send failed, aborting transaction: {}", e.getMessage());
                transactionAbortCounter.increment();
                throw e;
            }
        });
    }
}
