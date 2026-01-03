package com.example.logproducer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    private final KafkaProducerService kafkaProducerService;
    private final AtomicLong messageCounter = new AtomicLong(0);
    
    public LogEventController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }
    
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> publishLogEvent(@RequestBody LogEventRequest request) {
        long messageId = messageCounter.incrementAndGet();
        
        LogEvent event = LogEvent.builder()
            .messageId(String.valueOf(messageId))
            .level(request.getLevel())
            .service(request.getService())
            .message(request.getMessage())
            .timestamp(System.currentTimeMillis())
            .shouldFail(request.isShouldFail()) // For testing DLQ
            .build();
        
        kafkaProducerService.sendLogEvent(event);
        
        return ResponseEntity.ok(Map.of(
            "status", "published",
            "messageId", messageId,
            "topic", "log-events"
        ));
    }
    
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> publishBatch(@RequestBody BatchRequest request) {
        int count = request.getCount();
        int failureRate = request.getFailureRate(); // Percentage of messages that should fail
        
        for (int i = 0; i < count; i++) {
            long messageId = messageCounter.incrementAndGet();
            boolean shouldFail = (i % 100) < failureRate;
            
            LogEvent event = LogEvent.builder()
                .messageId(String.valueOf(messageId))
                .level("INFO")
                .service("batch-test")
                .message("Batch message " + i)
                .timestamp(System.currentTimeMillis())
                .shouldFail(shouldFail)
                .build();
            
            kafkaProducerService.sendLogEvent(event);
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "published",
            "count", count,
            "expectedFailures", (count * failureRate) / 100
        ));
    }
}
