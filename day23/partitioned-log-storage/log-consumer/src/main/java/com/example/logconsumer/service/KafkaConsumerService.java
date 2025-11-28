package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEntry;
import com.example.logconsumer.model.LogLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KafkaConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    @Autowired
    private BatchWriterService batchWriterService;
    
    private final ObjectMapper objectMapper;
    
    public KafkaConsumerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @KafkaListener(topics = "${kafka.topic.logs}", groupId = "log-consumer-group")
    public void consumeLog(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            // Parse timestamp - can be array [year, month, day, hour, minute, second, nanos] or ISO string
            LocalDateTime timestamp;
            JsonNode timestampNode = node.get("timestamp");
            if (timestampNode.isArray() && timestampNode.size() >= 6) {
                // Array format: [year, month, day, hour, minute, second, nanos]
                timestamp = LocalDateTime.of(
                    timestampNode.get(0).asInt(),  // year
                    timestampNode.get(1).asInt(),  // month
                    timestampNode.get(2).asInt(),  // day
                    timestampNode.get(3).asInt(),  // hour
                    timestampNode.get(4).asInt(),  // minute
                    timestampNode.get(5).asInt(),  // second
                    timestampNode.size() > 6 ? timestampNode.get(6).asInt() : 0  // nanos
                );
            } else {
                // String format: ISO-8601
                timestamp = LocalDateTime.parse(timestampNode.asText());
            }
            
            LogEntry logEntry = LogEntry.builder()
                .source(node.get("source").asText())
                .message(node.get("message").asText())
                .level(LogLevel.valueOf(node.get("level").asText()))
                .timestamp(timestamp)
                .traceId(node.get("traceId") != null ? node.get("traceId").asText() : null)
                .partitionKey(node.get("partitionKey") != null ? node.get("partitionKey").asText() : null)
                .sourceHash(node.get("sourceHash") != null ? node.get("sourceHash").asInt() : null)
                .build();
            
            batchWriterService.enqueue(logEntry);
            
        } catch (Exception e) {
            logger.error("Failed to process log message: {}", message, e);
        }
    }
}
