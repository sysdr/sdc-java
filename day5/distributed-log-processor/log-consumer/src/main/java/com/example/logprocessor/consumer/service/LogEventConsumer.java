package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LogEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);

    private final LogStorageService logStorageService;
    private final RotationPolicyService rotationPolicyService;
    private final ObjectMapper objectMapper;
    private final Counter logsConsumedCounter;
    private final Timer processingTimer;

    @Autowired
    public LogEventConsumer(LogStorageService logStorageService,
                           RotationPolicyService rotationPolicyService,
                           MeterRegistry meterRegistry) {
        this.logStorageService = logStorageService;
        this.rotationPolicyService = rotationPolicyService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        this.logsConsumedCounter = Counter.builder("logs_consumed_total")
                .description("Total number of log events consumed from Kafka")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("log_consumption_duration")
                .description("Time taken to process consumed log events")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    public void consumeLogEvent(String message) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.debug("Received log event: {}", message);
            
            // Parse the JSON message
            LogEvent logEvent = objectMapper.readValue(message, LogEvent.class);
            
            // Store the log event using the storage service
            logStorageService.storeLogEvent(logEvent);
            
            // Check if rotation is needed
            rotationPolicyService.evaluateRotationPolicies();
            
            logsConsumedCounter.increment();
            logger.debug("Successfully processed log event: trace_id={}", logEvent.getTraceId());
            
        } catch (Exception e) {
            logger.error("Failed to process log event: {}", message, e);
            // In production, you might want to send this to a dead letter queue
        } finally {
            sample.stop(processingTimer);
        }
    }
}
