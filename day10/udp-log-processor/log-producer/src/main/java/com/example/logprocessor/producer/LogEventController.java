package com.example.logprocessor.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogEventController {
    
    private final UdpLogShipperService udpShipper;
    
    @PostMapping("/ship")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> shipLog(
            @RequestBody LogEventRequest request) {
        
        LogEvent event = LogEvent.builder()
            .id(UUID.randomUUID().toString())
            .source(request.getSource())
            .level(request.getLevel())
            .message(request.getMessage())
            .timestamp(Instant.now())
            .build();
        
        return udpShipper.shipLog(event)
            .thenApply(v -> {
                Map<String, Object> response = Map.of(
                    "status", "shipped",
                    "id", event.getId(),
                    "sequenceNumber", event.getSequenceNumber()
                );
                return ResponseEntity.ok(response);
            })
            .exceptionally(e -> {
                log.error("Failed to ship log", e);
                Map<String, Object> errorResponse = Map.of("error", e.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(Map.of(
            "inFlightMessages", udpShipper.getInFlightCount()
        ));
    }
}

@lombok.Data
class LogEventRequest {
    private String source;
    private String level;
    private String message;
}
