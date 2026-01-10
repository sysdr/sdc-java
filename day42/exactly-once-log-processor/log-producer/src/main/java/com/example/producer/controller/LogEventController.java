package com.example.producer.controller;

import com.example.producer.model.LogEvent;
import com.example.producer.service.TransactionalProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogEventController {

    private final TransactionalProducerService producerService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createLogEvent(
            @RequestBody LogEvent event) {
        
        return producerService.sendLogEvent(event)
                .thenApply(result -> {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("eventId", event.getEventId());
                    response.put("partition", result.getRecordMetadata().partition());
                    response.put("offset", result.getRecordMetadata().offset());
                    response.put("timestamp", result.getRecordMetadata().timestamp());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    Map<String, Object> errorResponse = new java.util.HashMap<>();
                    errorResponse.put("error", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse);
                });
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatch(
            @RequestBody List<LogEvent> events) {
        try {
            producerService.sendBatchAtomic(events);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", "success");
            response.put("count", events.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Batch creation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new java.util.HashMap<>();
        response.put("status", "healthy");
        return ResponseEntity.ok(response);
    }
}
