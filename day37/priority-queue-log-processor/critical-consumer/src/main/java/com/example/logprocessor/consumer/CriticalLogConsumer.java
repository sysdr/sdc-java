package com.example.logprocessor.consumer;

import com.example.logprocessor.model.LogEvent;
import com.example.logprocessor.service.LogProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CriticalLogConsumer {
    private static final Logger logger = LoggerFactory.getLogger(CriticalLogConsumer.class);
    
    private final LogProcessingService processingService;
    private final ObjectMapper objectMapper;
    private final Timer processingTimer;
    private final Counter successCounter;
    private final Counter failureCounter;
    
    public CriticalLogConsumer(LogProcessingService processingService,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.processingService = processingService;
        this.objectMapper = objectMapper;
        
        this.processingTimer = Timer.builder("critical.processing.time")
            .register(meterRegistry);
        this.successCounter = Counter.builder("critical.processing.success")
            .register(meterRegistry);
        this.failureCounter = Counter.builder("critical.processing.failure")
            .register(meterRegistry);
    }
    
    @KafkaListener(
        topics = "critical-logs",
        groupId = "critical-consumer-group",
        containerFactory = "criticalKafkaListenerContainerFactory"
    )
    @CircuitBreaker(name = "criticalProcessor", fallbackMethod = "fallbackProcessing")
    public void consumeCritical(String message, Acknowledgment ack) {
        Timer.Sample sample = Timer.start();
        
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            event.setProcessedAt(Instant.now());
            
            logger.warn("🚨 CRITICAL LOG: {} - {}", event.getService(), event.getMessage());
            
            processingService.processCritical(event);
            
            ack.acknowledge();
            successCounter.increment();
            sample.stop(processingTimer);
            
        } catch (Exception e) {
            logger.error("Failed to process critical log", e);
            failureCounter.increment();
            // Don't ack - will retry
        }
    }
    
    @KafkaListener(
        topics = "high-logs",
        groupId = "critical-consumer-group",
        containerFactory = "criticalKafkaListenerContainerFactory"
    )
    public void consumeHigh(String message, Acknowledgment ack) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            event.setProcessedAt(Instant.now());
            
            logger.warn("⚠️  HIGH PRIORITY: {} - {}", event.getService(), event.getMessage());
            
            processingService.processHigh(event);
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("Failed to process high priority log", e);
        }
    }
    
    public void fallbackProcessing(String message, Acknowledgment ack, Exception e) {
        logger.error("Circuit breaker activated - routing to DLQ", e);
        // In production, send to dead letter queue
        ack.acknowledge();
    }
}
