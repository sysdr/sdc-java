package com.example.logprocessor.producer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    private final KafkaProducerService producerService;

    public LogEventController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, String>>> ingestLog(
            @RequestBody LogEventRequest request) {
        
        LogEvent event = new LogEvent(
            UUID.randomUUID().toString(),
            request.level(),
            request.message(),
            request.source(),
            Instant.now(),
            request.metadata()
        );

        return producerService.sendLog(event)
            .thenApply(result -> ResponseEntity.accepted()
                .body(Map.of(
                    "id", event.id(),
                    "status", "accepted",
                    "partition", String.valueOf(result.getRecordMetadata().partition())
                )))
            .exceptionally(ex -> ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to process log event",
                    "message", ex.getMessage()
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }
}

record LogEventRequest(
    String level,
    String message,
    String source,
    Map<String, Object> metadata
) {}
