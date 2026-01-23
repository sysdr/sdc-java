package com.example.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEventTimestampExtractor implements TimestampExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LogEventTimestampExtractor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        try {
            String json = (String) record.value();
            JsonNode node = objectMapper.readTree(json);
            
            // Extract event timestamp from the log event
            if (node.has("timestamp")) {
                return node.get("timestamp").asLong();
            }
        } catch (Exception e) {
            logger.warn("Failed to extract timestamp, using record timestamp: {}", e.getMessage());
        }
        
        // Fallback to Kafka record timestamp
        return record.timestamp();
    }
}
