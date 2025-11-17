package com.example.logprocessor.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Timer sendTimer;
    private final Counter successCounter;
    private final Counter failureCounter;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.sendTimer = Timer.builder("kafka.producer.send.time")
                .description("Time taken to send message to Kafka")
                .register(meterRegistry);
        this.successCounter = Counter.builder("kafka.producer.send.success")
                .description("Number of successful sends")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("kafka.producer.send.failure")
                .description("Number of failed sends")
                .register(meterRegistry);
    }

    public CompletableFuture<SendResult<String, String>> sendLog(LogEvent event) {
        return sendTimer.record(() -> {
            try {
                String message = objectMapper.writeValueAsString(event);
                String key = event.getId() != null ? event.getId() : UUID.randomUUID().toString();
                
                CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send("log-events", key, message);
                
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        successCounter.increment();
                        log.debug("Sent log to partition {} with offset {}", 
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        failureCounter.increment();
                        log.error("Failed to send log: {}", event, ex);
                    }
                });
                
                return future;
            } catch (Exception e) {
                failureCounter.increment();
                log.error("Error serializing log event", e);
                return CompletableFuture.failedFuture(e);
            }
        });
    }
}
