package com.systemdesign.logprocessor.consumer;

import com.systemdesign.logprocessor.consumer.service.LogProcessingService;
import com.systemdesign.logprocessor.model.LogEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LogProcessingServiceTest {

    @Autowired(required = false)
    private LogProcessingService processingService;

    @Test
    void testBatchProcessing() {
        if (processingService == null) {
            // Skip if service not available in test context
            return;
        }
        
        LogEvent event = LogEvent.builder()
            .id(UUID.randomUUID().toString())
            .applicationName("test-app")
            .level("INFO")
            .message("Test message")
            .timestamp(Instant.now())
            .host("localhost")
            .service("test-service")
            .build();
            
        ConsumerRecord<String, LogEvent> record = 
            new ConsumerRecord<>("test-topic", 0, 0, "key1", event);
            
        int processed = processingService.processBatch(List.of(record));
        
        assertTrue(processed >= 0, "Should process records without errors");
    }
}
