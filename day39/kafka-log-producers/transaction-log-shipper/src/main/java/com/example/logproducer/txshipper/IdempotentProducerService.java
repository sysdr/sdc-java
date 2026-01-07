package com.example.logproducer.txshipper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentProducerService {
    
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final TransactionOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;
    
    @Transactional
    public void sendTransaction(TransactionEvent event) {
        // Store in outbox first (transactional outbox pattern)
        TransactionOutbox outbox = TransactionOutbox.builder()
            .transactionId(event.getTransactionId())
            .userId(event.getUserId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .status("PENDING")
            .build();
        
        outboxRepository.save(outbox);
        
        // Send to Kafka with idempotent producer
        kafkaTemplate.send("transaction-logs", event.getTransactionId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    outbox.setStatus("SENT");
                    outboxRepository.save(outbox);
                    
                    Counter.builder("transactions.sent")
                        .tag("type", event.getType())
                        .register(meterRegistry)
                        .increment();
                    
                    log.info("Transaction {} sent successfully", event.getTransactionId());
                } else {
                    log.error("Failed to send transaction {}", event.getTransactionId(), ex);
                    
                    Counter.builder("transactions.failed")
                        .tag("type", event.getType())
                        .register(meterRegistry)
                        .increment();
                }
            });
    }
}
