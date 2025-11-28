package com.example.writegateway.service;

import com.example.writegateway.model.WriteRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WriteService {
    
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    @Value("${coordinator.url:http://localhost:8080}")
    private String coordinatorUrl;
    
    private final Counter writeSuccessCounter;
    private final Counter writeFailureCounter;
    private final Timer writeLatencyTimer;
    
    public WriteService(RestTemplate restTemplate,
                       RedisTemplate<String, Object> redisTemplate,
                       MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        
        this.writeSuccessCounter = Counter.builder("gateway.write.success")
            .description("Successful writes")
            .register(meterRegistry);
        
        this.writeFailureCounter = Counter.builder("gateway.write.failure")
            .description("Failed writes")
            .register(meterRegistry);
        
        this.writeLatencyTimer = Timer.builder("gateway.write.latency")
            .description("Write latency")
            .register(meterRegistry);
    }
    
    @CircuitBreaker(name = "writeService", fallbackMethod = "writeFallback")
    @Retry(name = "writeService")
    public Map<String, Object> write(WriteRequest request) {
        Instant start = Instant.now();
        
        try {
            // Get nodes for this key from coordinator
            String url = coordinatorUrl + "/api/coordinator/nodes/" + request.getKey() + "?count=3";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("nodes")) {
                throw new RuntimeException("Failed to get nodes from coordinator");
            }
            
            @SuppressWarnings("unchecked")
            List<String> nodeIds = (List<String>) response.get("nodes");
            
            if (nodeIds.isEmpty()) {
                throw new RuntimeException("No nodes available");
            }
            
            // Build write payload
            Map<String, Object> writePayload = new HashMap<>();
            writePayload.put("key", request.getKey());
            writePayload.put("content", request.getContent());
            writePayload.put("nodeId", nodeIds.get(0)); // Leader is first
            
            // Get follower URLs
            List<String> followers = nodeIds.subList(1, Math.min(3, nodeIds.size()))
                .stream()
                .map(nodeId -> getNodeUrl(nodeId))
                .toList();
            
            // Send write to leader with follower list
            String leaderUrl = getNodeUrl(nodeIds.get(0)) + "/api/storage/write" +
                "?followers=" + String.join(",", followers);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(writePayload, headers);
            
            Map<String, Object> writeResponse = restTemplate.postForObject(
                leaderUrl, entity, Map.class
            );
            
            Duration latency = Duration.between(start, Instant.now());
            writeLatencyTimer.record(latency);
            writeSuccessCounter.increment();
            
            log.info("Write successful: key={}, latency={}ms", 
                    request.getKey(), latency.toMillis());
            
            return Map.of(
                "success", true,
                "key", request.getKey(),
                "latencyMs", latency.toMillis(),
                "nodes", nodeIds
            );
            
        } catch (Exception e) {
            writeFailureCounter.increment();
            log.error("Write failed: key={}", request.getKey(), e);
            throw new RuntimeException("Write failed: " + e.getMessage(), e);
        }
    }
    
    private String getNodeUrl(String nodeId) {
        // In production, this would query Redis for node metadata
        // For simplicity, using naming convention
        if (nodeId.equals("node-1")) return "http://storage-node-1:8081";
        if (nodeId.equals("node-2")) return "http://storage-node-2:8082";
        if (nodeId.equals("node-3")) return "http://storage-node-3:8083";
        return "http://localhost:8081";
    }
    
    @SuppressWarnings("unused")
    private Map<String, Object> writeFallback(WriteRequest request, Exception e) {
        log.error("Write fallback triggered for key: {}", request.getKey(), e);
        return Map.of(
            "success", false,
            "key", request.getKey(),
            "error", "Service temporarily unavailable"
        );
    }
}
