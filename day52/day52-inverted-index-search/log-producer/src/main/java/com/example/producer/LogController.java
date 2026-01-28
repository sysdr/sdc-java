package com.example.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    
    private final KafkaProducerService producerService;
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateLogs(@RequestParam(defaultValue = "100") int count) {
        log.info("Generating {} log events", count);
        producerService.sendBatch(count);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "count", count,
            "message", "Log events sent to Kafka"
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "log-producer"));
    }
}
