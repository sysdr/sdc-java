package com.example.logprocessor.producer.controller;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.producer.service.LogProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/producer")
@RequiredArgsConstructor
public class ProducerController {

    private final LogProducerService producerService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendLogs(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "JSON") LogFormat format) {

        producerService.sendBatch(count, format);
        
        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "count", count,
                "format", format.name()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalSent", producerService.getMessageCount()
        ));
    }
}
