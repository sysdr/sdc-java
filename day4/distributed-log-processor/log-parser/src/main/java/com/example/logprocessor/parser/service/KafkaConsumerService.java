package com.example.logprocessor.parser.service;

import com.example.logprocessor.parser.model.ParsedLogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    @Autowired
    private LogParsingService logParsingService;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.kafka.topic.parsed-events}")
    private String parsedEventsTopic;
    
    @Value("${app.kafka.topic.parsing-dlq}")
    private String parsingDlqTopic;
    
    private final Counter messagesReceivedCounter;
    private final Counter messagesProcessedCounter;
    private final Counter messagesSentToDlqCounter;
    
    public KafkaConsumerService(MeterRegistry meterRegistry) {
        this.messagesReceivedCounter = Counter.builder("raw_logs_received_total")
            .description("Total number of raw log messages received")
            .register(meterRegistry);
        this.messagesProcessedCounter = Counter.builder("logs_processed_total")
            .description("Total number of logs successfully processed")
            .register(meterRegistry);
        this.messagesSentToDlqCounter = Counter.builder("logs_sent_to_dlq_total")
            .description("Total number of logs sent to dead letter queue")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "${app.kafka.topic.raw-logs}", concurrency = "3")
    public void processRawLog(String rawLogEntry) {
        messagesReceivedCounter.increment();
        logger.debug("Processing raw log: {}", rawLogEntry);
        
        try {
            ParsedLogEvent parsedEvent = logParsingService.parseLogEntry(rawLogEntry);
            
            if ("failed".equals(parsedEvent.getLogFormat())) {
                // Send to dead letter queue
                sendToDeadLetterQueue(rawLogEntry, parsedEvent);
            } else {
                // Send to parsed events topic
                sendParsedEvent(parsedEvent);
                messagesProcessedCounter.increment();
            }
            
        } catch (Exception e) {
            logger.error("Error processing raw log entry", e);
            sendToDeadLetterQueue(rawLogEntry, null);
        }
    }
    
    private void sendParsedEvent(ParsedLogEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(parsedEventsTopic, event.getIpAddress(), eventJson);
            logger.debug("Sent parsed event for IP: {}", event.getIpAddress());
        } catch (Exception e) {
            logger.error("Failed to send parsed event", e);
            throw new RuntimeException("Failed to send parsed event", e);
        }
    }
    
    private void sendToDeadLetterQueue(String rawLog, ParsedLogEvent failedEvent) {
        try {
            messagesSentToDlqCounter.increment();
            
            // Create DLQ message with metadata
            String dlqMessage = String.format("{\"raw_log\":\"%s\",\"failure_reason\":\"%s\",\"timestamp\":\"%s\"}",
                rawLog.replace("\"", "\\\""),
                failedEvent != null ? failedEvent.getMetadata().getOrDefault("parse_failure_reason", "Unknown") : "Processing error",
                java.time.LocalDateTime.now().toString());
            
            kafkaTemplate.send(parsingDlqTopic, dlqMessage);
            logger.warn("Sent message to DLQ: {}", rawLog);
        } catch (Exception e) {
            logger.error("Failed to send message to DLQ", e);
        }
    }
}
