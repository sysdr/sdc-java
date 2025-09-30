package com.example.logprocessor.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC_NAME = "log-events";
    
    @Autowired
    private KafkaTemplate<String, LogEvent> kafkaTemplate;
    
    private final AtomicLong sentEvents = new AtomicLong(0);
    private final AtomicLong failedEvents = new AtomicLong(0);

    public void sendLogEvent(LogEvent logEvent) {
        try {
            org.springframework.util.concurrent.ListenableFuture<SendResult<String, LogEvent>> future = 
                kafkaTemplate.send(TOPIC_NAME, logEvent.getId(), logEvent);
            
            future.addCallback(
                result -> {
                    logger.debug("Successfully sent log event: {}", logEvent.getId());
                    sentEvents.incrementAndGet();
                },
                throwable -> {
                    logger.error("Failed to send log event: {}", logEvent.getId(), throwable);
                    failedEvents.incrementAndGet();
                }
            );
        } catch (Exception e) {
            logger.error("Error sending log event to Kafka", e);
            throw e;
        }
    }

    public void fallbackSendLogEvent(LogEvent logEvent, Exception ex) {
        logger.warn("Circuit breaker activated for log event: {}. Reason: {}", 
                   logEvent.getId(), ex.getMessage());
        // Could implement local persistence here as fallback
        failedEvents.incrementAndGet();
    }

    public long getSentEventsCount() {
        return sentEvents.get();
    }

    public long getFailedEventsCount() {
        return failedEvents.get();
    }
}
