package com.example.logprocessor.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendCustomLog(@RequestBody String logEntry) {
        kafkaProducerService.sendRawLog(logEntry);
        return ResponseEntity.ok(Map.of("status", "sent", "message", "Log entry queued for processing"));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "log-producer"));
    }
}
