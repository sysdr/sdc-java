package com.example.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DLQManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(DLQManagementService.class);
    private static final String DLQ_TOPIC = "log-events-dlq";
    
    private final KafkaConsumer<String, String> dlqConsumer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public DLQManagementService(KafkaConsumer<String, String> dlqConsumer,
                               KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.dlqConsumer = dlqConsumer;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    public List<DLQMessage> getDLQMessages(String errorType, int offset, int limit) {
        List<DLQMessage> messages = new ArrayList<>();
        
        synchronized (dlqConsumer) {
            dlqConsumer.subscribe(Collections.singletonList(DLQ_TOPIC));
            ConsumerRecords<String, String> records = dlqConsumer.poll(Duration.ofSeconds(5));
            
            for (ConsumerRecord<String, String> record : records) {
                var errorTypeHeader = record.headers().lastHeader("error-type");
                String recErrorType = errorTypeHeader != null ? new String(errorTypeHeader.value()) : "UNKNOWN";
                
                if (errorType == null || errorType.equals(recErrorType)) {
                    DLQMessage msg = convertToDLQMessage(record);
                    messages.add(msg);
                }
                
                if (messages.size() >= limit) break;
            }
        }
        
        return messages;
    }
    
    public Optional<DLQMessage> getDLQMessage(String messageId) {
        return getDLQMessages(null, 0, 1000).stream()
            .filter(msg -> msg.getMessageId().equals(messageId))
            .findFirst();
    }
    
    public boolean reprocessMessage(String messageId) {
        Optional<DLQMessage> messageOpt = getDLQMessage(messageId);
        
        if (messageOpt.isPresent()) {
            DLQMessage dlqMessage = messageOpt.get();
            kafkaTemplate.send(dlqMessage.getOriginalTopic(), 
                             messageId, 
                             dlqMessage.getPayload());
            log.info("Reprocessing message: {}", messageId);
            return true;
        }
        
        return false;
    }
    
    public int reprocessBatch(String errorType, int limit) {
        List<DLQMessage> messages = getDLQMessages(errorType, 0, limit);
        
        for (DLQMessage msg : messages) {
            kafkaTemplate.send(msg.getOriginalTopic(), 
                             msg.getMessageId(), 
                             msg.getPayload());
        }
        
        log.info("Reprocessed {} messages from DLQ", messages.size());
        return messages.size();
    }
    
    public Map<String, Object> getDLQStats() {
        List<DLQMessage> allMessages = getDLQMessages(null, 0, 10000);
        
        Map<String, Long> byErrorType = allMessages.stream()
            .collect(Collectors.groupingBy(
                DLQMessage::getErrorType, 
                Collectors.counting()
            ));
        
        return Map.of(
            "totalMessages", allMessages.size(),
            "byErrorType", byErrorType,
            "oldestMessage", allMessages.stream()
                .mapToLong(DLQMessage::getDlqTimestamp)
                .min().orElse(0L),
            "newestMessage", allMessages.stream()
                .mapToLong(DLQMessage::getDlqTimestamp)
                .max().orElse(0L)
        );
    }
    
    private DLQMessage convertToDLQMessage(ConsumerRecord<String, String> record) {
        String errorType = getHeaderValue(record, "error-type");
        String errorMessage = getHeaderValue(record, "error-message");
        String retryCountStr = getHeaderValue(record, "retry-count");
        String timestampStr = getHeaderValue(record, "dlq-timestamp");
        String originalTopic = getHeaderValue(record, "original-topic");
        
        return new DLQMessage(
            record.key(),
            record.value(),
            errorType != null ? errorType : "UNKNOWN",
            errorMessage != null ? errorMessage : "No error message",
            retryCountStr != null ? Integer.parseInt(retryCountStr) : 0,
            timestampStr != null ? Long.parseLong(timestampStr) : 0L,
            originalTopic != null ? originalTopic : "log-events"
        );
    }
    
    private String getHeaderValue(ConsumerRecord<String, String> record, String key) {
        var header = record.headers().lastHeader(key);
        return header != null ? new String(header.value()) : null;
    }
}
