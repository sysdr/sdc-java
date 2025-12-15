package com.logprocessor.repair;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadRepairService {
    
    private final WebClient webClient;
    private final Counter repairsTriggered;
    private final Counter repairsCompleted;
    
    @Value("${storage.nodes}")
    private String storageNodes;
    
    public ReadRepairService(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.build();
        this.repairsTriggered = Counter.builder("read_repairs_triggered_total")
            .description("Total read repairs triggered")
            .register(meterRegistry);
        this.repairsCompleted = Counter.builder("read_repairs_completed_total")
            .description("Total read repairs completed")
            .register(meterRegistry);
    }
    
    public Map<String, Object> readWithRepair(String partitionId, Long version) {
        List<String> nodes = Arrays.asList(storageNodes.split(","));
        
        // Read from all replicas
        List<Map<String, Object>> responses = new ArrayList<>();
        for (String node : nodes) {
            try {
                Map<String, Object> response = webClient.get()
                    .uri(node + "/api/storage/read/" + partitionId + "/" + version)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                
                if (response != null) {
                    response.put("sourceNode", node);
                    responses.add(response);
                }
            } catch (Exception e) {
                // Node unavailable, skip
            }
        }
        
        if (responses.isEmpty()) {
            return Map.of("status", "error", "message", "No replicas available");
        }
        
        // Detect inconsistencies
        Map<String, Object> canonical = selectCanonicalVersion(responses);
        List<String> staleNodes = findStaleNodes(responses, canonical);
        
        if (!staleNodes.isEmpty()) {
            // Trigger async repair
            triggerRepairAsync(partitionId, version, canonical, staleNodes);
        }
        
        return canonical;
    }
    
    private Map<String, Object> selectCanonicalVersion(List<Map<String, Object>> responses) {
        // Select version with highest Lamport clock
        return responses.stream()
            .max(Comparator.comparing(r -> ((Number) r.get("lamportClock")).longValue()))
            .orElse(responses.get(0));
    }
    
    private List<String> findStaleNodes(List<Map<String, Object>> responses, Map<String, Object> canonical) {
        Long canonicalClock = ((Number) canonical.get("lamportClock")).longValue();
        
        return responses.stream()
            .filter(r -> ((Number) r.get("lamportClock")).longValue() < canonicalClock)
            .map(r -> (String) r.get("sourceNode"))
            .collect(Collectors.toList());
    }
    
    @Async
    public void triggerRepairAsync(String partitionId, Long version, 
                                   Map<String, Object> canonical, List<String> staleNodes) {
        repairsTriggered.increment();
        
        for (String node : staleNodes) {
            try {
                Map<String, Object> writeRequest = Map.of(
                    "partitionId", partitionId,
                    "message", canonical.get("message"),
                    "lamportClock", canonical.get("lamportClock")
                );
                
                webClient.post()
                    .uri(node + "/api/storage/write")
                    .body(BodyInserters.fromValue(writeRequest))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                
                repairsCompleted.increment();
            } catch (Exception e) {
                // Log repair failure
            }
        }
    }
}
