package com.example.logprocessor.producer.controller;

import com.example.logprocessor.producer.model.LogEvent;
import com.example.logprocessor.producer.service.RabbitMQProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final RabbitMQProducerService producerService;

    @PostMapping
    public ResponseEntity<Map<String, String>> publishLog(@RequestBody LogEvent logEvent) {
        producerService.publishLog(logEvent);
        return ResponseEntity.ok(Map.of(
            "status", "published",
            "id", logEvent.getId()
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> publishBatch(@RequestBody List<LogEvent> logEvents) {
        logEvents.forEach(producerService::publishLog);
        return ResponseEntity.ok(Map.of(
            "status", "published",
            "count", logEvents.size()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
