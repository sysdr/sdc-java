package com.example.logproducer.controller;

import com.example.logproducer.model.LogEvent;
import com.example.logproducer.model.LogLevel;
import com.example.logproducer.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    @PostMapping
    public ResponseEntity<String> ingestLog(@RequestBody LogEvent logEvent) {
        // Enrich with server-side metadata
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(LocalDateTime.now());
        }
        if (logEvent.getTraceId() == null) {
            logEvent.setTraceId(UUID.randomUUID().toString());
        }
        
        kafkaProducerService.sendLog(logEvent);
        return ResponseEntity.accepted().body(logEvent.getTraceId());
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
