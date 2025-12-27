package com.example.logproducer.service;

import com.example.logproducer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final Counter sentCounter;
    private final Counter failedCounter;

    public KafkaProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.sentCounter = meterRegistry.counter("producer.messages.sent");
        this.failedCounter = meterRegistry.counter("producer.messages.failed");
    }

    public CompletableFuture<SendResult<String, LogEvent>> sendLog(LogEvent event) {
        CompletableFuture<SendResult<String, LogEvent>> future =
                kafkaTemplate.send("log-events", event.getId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                sentCounter.increment();
                log.debug("Message sent successfully: id={}, partition={}, offset={}",
                        event.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                failedCounter.increment();
                log.error("Failed to send message: id={}", event.getId(), ex);
            }
        });

        return future;
    }
}
