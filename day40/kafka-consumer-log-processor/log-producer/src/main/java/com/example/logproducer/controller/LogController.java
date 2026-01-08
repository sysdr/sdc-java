package com.example.logproducer.controller;

import com.example.logproducer.model.LogEvent;
import com.example.logproducer.service.LogProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogProducerService producerService;

    @PostMapping
    public ResponseEntity<String> createLog(@RequestBody LogEvent logEvent) {
        if (logEvent.getId() == null) {
            logEvent.setId(UUID.randomUUID().toString());
        }
        if (logEvent.getTimestamp() == null) {
            logEvent.setTimestamp(Instant.now());
        }
        
        producerService.sendLog(logEvent);
        return ResponseEntity.accepted().body(logEvent.getId());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer healthy");
    }
}
