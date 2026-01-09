package com.example.logprocessor.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
@RequiredArgsConstructor
public class ConsumerHealthController {

    private final ConsumerMetricsService metricsService;
    private final ProcessedLogRepository repository;
    private final KafkaListenerEndpointRegistry registry;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean allRunning = registry.getAllListenerContainers()
                .stream()
                .allMatch(container -> container.isRunning());
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", allRunning ? "UP" : "DOWN");
        health.put("consumers", registry.getAllListenerContainers().size());
        health.put("totalProcessed", repository.count());
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/lag")
    public ResponseEntity<Map<Integer, Long>> getLag() {
        return ResponseEntity.ok(metricsService.getCurrentLag());
    }

    @GetMapping("/partition-distribution")
    public ResponseEntity<List<Map<String, Object>>> getPartitionDistribution() {
        return ResponseEntity.ok(repository.getPartitionDistribution());
    }

    @GetMapping("/assigned-partitions")
    public ResponseEntity<Map<String, Object>> getAssignedPartitions() {
        Map<String, Object> result = new HashMap<>();
        registry.getAllListenerContainers().forEach(container -> {
            result.put(container.getListenerId(), 
                      container.getAssignedPartitions().size());
        });
        return ResponseEntity.ok(result);
    }
}
