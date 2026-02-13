package com.example.producer.controller;

import com.example.producer.model.LogEvent;
import com.example.producer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogEventController {
    
    private final KafkaProducerService kafkaProducerService;
    
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestLog(@RequestBody LogEvent logEvent) {
        // Add metadata
        logEvent.setEventId(UUID.randomUUID().toString());
        logEvent.setTimestamp(Instant.now());
        
        kafkaProducerService.sendLogEvent(logEvent);
        
        return ResponseEntity.ok("Log event queued: " + logEvent.getEventId());
    }
    
    @PostMapping("/bulk")
    public ResponseEntity<String> ingestBulk(@RequestBody java.util.List<LogEvent> events) {
        events.forEach(event -> {
            event.setEventId(UUID.randomUUID().toString());
            event.setTimestamp(Instant.now());
            kafkaProducerService.sendLogEvent(event);
        });
        
        return ResponseEntity.ok("Bulk ingested " + events.size() + " events");
    }
}
