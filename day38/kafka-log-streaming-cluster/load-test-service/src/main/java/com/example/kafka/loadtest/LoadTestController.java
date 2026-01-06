package com.example.kafka.loadtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loadtest")
public class LoadTestController {

    @Autowired
    private LogProducerService producerService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runLoadTest(
            @RequestParam(defaultValue = "10000") int eventCount,
            @RequestParam(defaultValue = "log-events") String topic) {
        
        long startTime = System.currentTimeMillis();
        producerService.sendLogBatch(eventCount, topic);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("eventCount", eventCount);
        response.put("topic", topic);
        response.put("durationMs", duration);
        response.put("throughput", (eventCount * 1000.0) / duration);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSent", producerService.getTotalSent());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetStats() {
        producerService.resetCounters();
        return ResponseEntity.ok("Counters reset");
    }
}
