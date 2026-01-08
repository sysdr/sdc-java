package com.example.logproducer.service;

import com.example.logproducer.model.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class LogProducerService {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;

    public LogProducerService(KafkaTemplate<String, LogEvent> kafkaTemplate,
                             MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.successCounter = Counter.builder("log.producer.success")
                .description("Successful log productions")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("log.producer.failure")
                .description("Failed log productions")
                .register(meterRegistry);
    }

    public CompletableFuture<SendResult<String, LogEvent>> sendLog(LogEvent logEvent) {
        return kafkaTemplate.send("application-logs", logEvent.getId(), logEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        failureCounter.increment();
                        log.error("Failed to send log: {}", logEvent.getId(), ex);
                    } else {
                        successCounter.increment();
                        log.debug("Log sent successfully: partition={}, offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
