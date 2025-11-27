package com.example.producer.controller;

import com.example.producer.model.LogEvent;
import com.example.producer.service.LogProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {
    
    private final LogProducerService logProducerService;
    
    @PostMapping
    public ResponseEntity<String> sendLog(@RequestBody LogEvent logEvent) {
        if (logEvent.getId() == null) {
            logEvent.setId(UUID.randomUUID().toString());
        }
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(Instant.now());
        }
        if (logEvent.getLogSchemaVersion() == null) {
            logEvent.setLogSchemaVersion("v2.0");
        }
        
        logProducerService.sendLog(logEvent);
        return ResponseEntity.ok("Log sent: " + logEvent.getId());
    }
}
