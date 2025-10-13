package com.example.logprocessor.producer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogEventController {

    private final KafkaProducerService producerService;

    public LogEventController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createLog(@RequestBody Map<String, Object> logData) {
        String logJson = buildLogJson(logData);
        producerService.sendLog(logJson);
        
        return ResponseEntity.ok(Map.of(
            "status", "accepted",
            "timestamp", Instant.now().toString()
        ));
    }

    private String buildLogJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"timestamp\":\"").append(Instant.now()).append("\",");
        json.append("\"level\":\"").append(data.getOrDefault("level", "INFO")).append("\",");
        json.append("\"service\":\"").append(data.getOrDefault("service", "unknown")).append("\",");
        json.append("\"message\":\"").append(data.getOrDefault("message", "")).append("\",");
        json.append("\"metadata\":").append(serializeMetadata(data));
        json.append("}");
        return json.toString();
    }

    private String serializeMetadata(Map<String, Object> data) {
        return "{\"user_id\":\"" + data.getOrDefault("user_id", "anonymous") + "\","
            + "\"request_id\":\"" + data.getOrDefault("request_id", "") + "\"}";
    }
}
