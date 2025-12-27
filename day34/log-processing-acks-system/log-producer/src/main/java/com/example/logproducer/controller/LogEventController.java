package com.example.logproducer.controller;

import com.example.logproducer.model.LogEvent;
import com.example.logproducer.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogEventController {

    private final KafkaProducerService producerService;

    @PostMapping
    public ResponseEntity<String> createLog(@RequestBody LogEvent event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        producerService.sendLog(event);
        return ResponseEntity.accepted().body(event.getId());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer service is healthy");
    }
}
