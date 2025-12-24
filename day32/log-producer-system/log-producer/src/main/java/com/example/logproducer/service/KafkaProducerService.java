package com.example.logproducer.service;

import com.example.logproducer.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private static final String TOPIC = "logs";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    private Counter messagesSent;
    private Counter messagesFailedCounter;
    private Timer sendTimer;
    
    @PostConstruct
    public void init() {
        // Initialize metrics
        this.messagesSent = Counter.builder("kafka.producer.messages.sent")
            .description("Total messages sent to Kafka")
            .tag("topic", TOPIC)
            .register(meterRegistry);
            
        this.messagesFailedCounter = Counter.builder("kafka.producer.messages.failed")
            .description("Total messages failed to send")
            .tag("topic", TOPIC)
            .register(meterRegistry);
            
        this.sendTimer = Timer.builder("kafka.producer.send.duration")
            .description("Time taken to send messages")
            .tag("topic", TOPIC)
            .register(meterRegistry);
    }
    
    /**
     * Fire-and-forget: Maximum throughput, no delivery guarantee
     */
    public void sendFireAndForget(LogEvent logEvent) {
        try {
            String message = objectMapper.writeValueAsString(logEvent);
            kafkaTemplate.send(TOPIC, logEvent.getEventId(), message);
            messagesSent.increment();
            
            log.debug("Sent log event (fire-and-forget): {}", logEvent.getEventId());
        } catch (Exception e) {
            messagesFailedCounter.increment();
            log.error("Failed to send log event: {}", logEvent.getEventId(), e);
            throw new RuntimeException("Failed to send log event", e);
        }
    }
    
    /**
     * Synchronous send: Wait for acknowledgment, guaranteed delivery
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "fallbackSendSync")
    @Retry(name = "kafkaProducer")
    public void sendSync(LogEvent logEvent) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            String message = objectMapper.writeValueAsString(logEvent);
            SendResult<String, String> result = kafkaTemplate
                .send(TOPIC, logEvent.getEventId(), message)
                .get(5, TimeUnit.SECONDS);
            
            messagesSent.increment();
            sample.stop(sendTimer);
            
            log.info("Sent log event (sync): {} to partition {} with offset {}",
                logEvent.getEventId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
                
        } catch (Exception e) {
            messagesFailedCounter.increment();
            sample.stop(sendTimer);
            log.error("Failed to send log event synchronously: {}", logEvent.getEventId(), e);
            throw new RuntimeException("Failed to send log event", e);
        }
    }
    
    /**
     * Asynchronous send with callback: Balance throughput and reliability
     */
    @CircuitBreaker(name = "kafkaProducer")
    public CompletableFuture<SendResult<String, String>> sendAsync(LogEvent logEvent) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            String message = objectMapper.writeValueAsString(logEvent);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(TOPIC, logEvent.getEventId(), message);
            
            future.whenComplete((result, ex) -> {
                sample.stop(sendTimer);
                
                if (ex == null) {
                    messagesSent.increment();
                    log.debug("Sent log event (async): {} to partition {}",
                        logEvent.getEventId(),
                        result.getRecordMetadata().partition());
                } else {
                    messagesFailedCounter.increment();
                    log.error("Failed to send log event asynchronously: {}", 
                        logEvent.getEventId(), ex);
                }
            });
            
            return future;
            
        } catch (Exception e) {
            messagesFailedCounter.increment();
            sample.stop(sendTimer);
            log.error("Failed to serialize log event: {}", logEvent.getEventId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Circuit breaker fallback for synchronous sends
     */
    private void fallbackSendSync(LogEvent logEvent, Exception e) {
        log.error("Circuit breaker open - dropping log event: {}", logEvent.getEventId(), e);
        messagesFailedCounter.increment();
        // In production, might want to write to local buffer or dead letter queue
    }
    
    public boolean isHealthy() {
        // Check if we can send a test message
        try {
            LogEvent testEvent = LogEvent.builder()
                .eventId("health-check")
                .source("health-check")
                .level(LogEvent.LogLevel.DEBUG)
                .message("Health check")
                .build();
            
            sendSync(testEvent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("messagesSent", messagesSent.count());
        metrics.put("messagesFailed", messagesFailedCounter.count());
        metrics.put("avgSendDuration", sendTimer.mean(TimeUnit.MILLISECONDS));
        return metrics;
    }
}
