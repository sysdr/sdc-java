package com.example.logproducer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogProducerController {

    private final LogProducerService producerService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateBurstLogs(@RequestParam(defaultValue = "100") int count) {
        for (int i = 0; i < count; i++) {
            producerService.generateLogEvent();
        }
        return ResponseEntity.ok("Generated " + count + " log events");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer is healthy");
    }
}
