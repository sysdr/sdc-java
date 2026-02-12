package com.example.logprocessor.consumer;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.service.LogPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class LogEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);
    
    private final LogPersistenceService persistenceService;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Timer processingTimer;

    public LogEventConsumer(LogPersistenceService persistenceService, 
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.persistenceService = persistenceService;
        this.objectMapper = objectMapper;
        this.processedCounter = meterRegistry.counter("log.events.processed");
        this.failedCounter = meterRegistry.counter("log.events.failed");
        this.processingTimer = meterRegistry.timer("log.events.processing.time");
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group", concurrency = "3")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        Instant startTime = Instant.now();
        
        try {
            LogEvent event = objectMapper.readValue(record.value(), LogEvent.class);
            logger.debug("Consuming log event: {}", event.getId());
            
            persistenceService.persistLog(event);
            
            processedCounter.increment();
            acknowledgment.acknowledge();
            
            long processingTimeMs = Duration.between(startTime, Instant.now()).toMillis();
            processingTimer.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            logger.debug("Successfully processed log: {} in {}ms", event.getId(), processingTimeMs);
            
        } catch (Exception e) {
            failedCounter.increment();
            logger.error("Failed to process log event from partition {} offset {}: {}", 
                record.partition(), record.offset(), e.getMessage(), e);
            
            // In production: send to DLQ after retry exhaustion
            // For now: acknowledge to prevent blocking
            acknowledgment.acknowledge();
        }
    }
}
