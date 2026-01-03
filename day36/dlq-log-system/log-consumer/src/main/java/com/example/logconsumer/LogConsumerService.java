package com.example.logconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LogConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(LogConsumerService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final ObjectMapper objectMapper;
    private final LogRepository logRepository;
    private final DeadLetterQueueService dlqService;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    
    public LogConsumerService(ObjectMapper objectMapper,
                             LogRepository logRepository,
                             DeadLetterQueueService dlqService,
                             MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
        this.dlqService = dlqService;
        this.messagesProcessed = Counter.builder("consumer.messages.processed")
            .description("Total messages successfully processed")
            .register(meterRegistry);
        this.messagesFailed = Counter.builder("consumer.messages.failed")
            .description("Total messages failed processing")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    public void consumeLogEvent(@Payload String message,
                               @Header(value = "retry-count", required = false) Integer retryCount,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        int currentRetryCount = retryCount != null ? retryCount : 0;
        
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            
            // Simulate different failure scenarios for testing
            if (event.isShouldFail()) {
                if (event.getMessage().contains("timeout")) {
                    throw new TransientException("Simulated timeout error");
                } else if (event.getMessage().contains("validation")) {
                    throw new PermanentException("Simulated validation error");
                } else {
                    throw new RuntimeException("Simulated generic error");
                }
            }
            
            // Process the log event
            processLogEvent(event);
            messagesProcessed.increment();
            
        } catch (TransientException e) {
            handleTransientFailure(message, currentRetryCount, e, key);
        } catch (PermanentException e) {
            handlePermanentFailure(message, e, key);
        } catch (Exception e) {
            handleAmbiguousFailure(message, currentRetryCount, e, key);
        }
    }
    
    private void processLogEvent(LogEvent event) {
        // Persist to database
        LogEntity entity = new LogEntity();
        entity.setMessageId(event.getMessageId());
        entity.setLevel(event.getLevel());
        entity.setService(event.getService());
        entity.setMessage(event.getMessage());
        entity.setTimestamp(event.getTimestamp());
        entity.setProcessedAt(System.currentTimeMillis());
        
        logRepository.save(entity);
        log.info("Processed log event: {}", event.getMessageId());
    }
    
    private void handleTransientFailure(String message, int retryCount, Exception e, String key) {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            log.warn("Transient failure, will retry. Attempt {}/{}", retryCount + 1, MAX_RETRY_ATTEMPTS);
            dlqService.sendToRetryTopic(message, retryCount + 1, "TRANSIENT", e.getMessage(), key);
        } else {
            log.error("Max retries exceeded for transient error, sending to DLQ");
            messagesFailed.increment();
            dlqService.sendToDeadLetterQueue(message, retryCount, "TIMEOUT", e.getMessage(), key);
        }
    }
    
    private void handlePermanentFailure(String message, Exception e, String key) {
        log.error("Permanent failure, sending directly to DLQ: {}", e.getMessage());
        messagesFailed.increment();
        dlqService.sendToDeadLetterQueue(message, 0, "VALIDATION", e.getMessage(), key);
    }
    
    private void handleAmbiguousFailure(String message, int retryCount, Exception e, String key) {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            log.warn("Ambiguous failure, will retry. Attempt {}/{}", retryCount + 1, MAX_RETRY_ATTEMPTS);
            dlqService.sendToRetryTopic(message, retryCount + 1, "AMBIGUOUS", e.getMessage(), key);
        } else {
            log.error("Max retries exceeded for ambiguous error, sending to DLQ");
            messagesFailed.increment();
            dlqService.sendToDeadLetterQueue(message, retryCount, "PROCESSING", e.getMessage(), key);
        }
    }
}
