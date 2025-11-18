package com.example.logprocessor.consumer.listener;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/consumer")
public class ConsumerStatusController {

    private final AvroLogEventListener listener;
    private final RedisTemplate<String, String> redisTemplate;

    public ConsumerStatusController(
            AvroLogEventListener listener,
            RedisTemplate<String, String> redisTemplate) {
        this.listener = listener;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("correlationCounts", listener.getCorrelationCounts());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/logs/{eventId}")
    public ResponseEntity<Map<Object, Object>> getLogEvent(@PathVariable String eventId) {
        String redisKey = "log:" + eventId;
        Map<Object, Object> event = redisTemplate.opsForHash().entries(redisKey);
        
        if (event.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(event);
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<Map<String, Object>> getByCorrelation(
            @PathVariable String correlationId) {
        
        Set<String> eventIds = redisTemplate.opsForSet()
            .members("correlation:" + correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("eventCount", eventIds != null ? eventIds.size() : 0);
        response.put("eventIds", eventIds);
        
        return ResponseEntity.ok(response);
    }
}
