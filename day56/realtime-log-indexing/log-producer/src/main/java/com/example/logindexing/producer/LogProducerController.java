package com.example.logindexing.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogProducerController {

    private final LogProducerService producerService;

    public LogProducerController(LogProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> produceLog(@RequestBody LogEvent logEvent) {
        producerService.produceLog(logEvent);
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "logId", logEvent.getId()));
    }

    @PostMapping("/batch/{count}")
    public ResponseEntity<Map<String, Object>> produceBatch(@PathVariable int count) {
        for (int i = 0; i < count; i++) {
            LogEvent logEvent = producerService.generateRandomLog();
            producerService.produceLog(logEvent);
        }
        return ResponseEntity.ok(Map.of("status", "success", "count", count));
    }
}
