package com.example.readgateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ReadService {
    
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    
    @Value("${coordinator.url:http://localhost:8080}")
    private String coordinatorUrl;
    
    @Value("${read.quorum:2}")
    private int readQuorum;
    
    private final Counter readSuccessCounter;
    private final Counter readFailureCounter;
    private final Timer readLatencyTimer;
    
    public ReadService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        
        this.readSuccessCounter = Counter.builder("gateway.read.success")
            .description("Successful reads")
            .register(meterRegistry);
        
        this.readFailureCounter = Counter.builder("gateway.read.failure")
            .description("Failed reads")
            .register(meterRegistry);
        
        this.readLatencyTimer = Timer.builder("gateway.read.latency")
            .description("Read latency")
            .register(meterRegistry);
    }
    
    public Map<String, Object> read(String key) {
        Instant start = Instant.now();
        
        try {
            // Get nodes for this key
            String url = coordinatorUrl + "/api/coordinator/nodes/" + key + "?count=3";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || !response.containsKey("nodes")) {
                throw new RuntimeException("Failed to get nodes from coordinator");
            }
            
            @SuppressWarnings("unchecked")
            List<String> nodeIds = (List<String>) response.get("nodes");
            
            // Scatter-gather read from multiple nodes
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (String nodeId : nodeIds) {
                CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String nodeUrl = getNodeUrl(nodeId) + "/api/storage/read/" + key;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = restTemplate.getForObject(nodeUrl, Map.class);
                        return result;
                    } catch (Exception e) {
                        log.warn("Failed to read from node: {}", nodeId, e);
                        return null;
                    }
                });
                futures.add(future);
            }
            
            // Wait for read quorum
            int successfulReads = 0;
            Map<String, Object> latestResult = null;
            long latestVersion = 0;
            
            for (CompletableFuture<Map<String, Object>> future : futures) {
                try {
                    Map<String, Object> result = future.get();
                    if (result != null) {
                        successfulReads++;
                        Number version = (Number) result.get("version");
                        if (version != null && version.longValue() > latestVersion) {
                            latestVersion = version.longValue();
                            latestResult = result;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error getting read result", e);
                }
            }
            
            if (successfulReads < readQuorum) {
                throw new RuntimeException("Failed to achieve read quorum");
            }
            
            Duration latency = Duration.between(start, Instant.now());
            readLatencyTimer.record(latency);
            readSuccessCounter.increment();
            
            log.info("Read successful: key={}, latency={}ms, quorum={}/{}", 
                    key, latency.toMillis(), successfulReads, readQuorum);
            
            return Map.of(
                "success", true,
                "key", key,
                "data", latestResult != null ? latestResult : Map.of(),
                "latencyMs", latency.toMillis(),
                "replicas", successfulReads
            );
            
        } catch (Exception e) {
            readFailureCounter.increment();
            log.error("Read failed: key={}", key, e);
            throw new RuntimeException("Read failed: " + e.getMessage(), e);
        }
    }
    
    private String getNodeUrl(String nodeId) {
        if (nodeId.equals("node-1")) return "http://storage-node-1:8081";
        if (nodeId.equals("node-2")) return "http://storage-node-2:8082";
        if (nodeId.equals("node-3")) return "http://storage-node-3:8083";
        return "http://localhost:8081";
    }
}
