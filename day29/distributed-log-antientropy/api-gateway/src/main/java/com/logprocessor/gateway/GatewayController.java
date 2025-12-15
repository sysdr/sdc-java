package com.logprocessor.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import java.util.*;

@RestController
@RequestMapping("/api")
public class GatewayController {
    
    private final WebClient webClient;
    
    @Value("${read.repair.service.url}")
    private String readRepairServiceUrl;
    
    @Value("${storage.nodes}")
    private String storageNodes;
    
    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> write(@RequestBody WriteRequest request) {
        List<String> nodes = Arrays.asList(storageNodes.split(","));
        List<Map<String, Object>> responses = new ArrayList<>();
        
        for (String node : nodes) {
            try {
                Map<String, Object> response = webClient.post()
                    .uri(node + "/api/storage/write")
                    .body(BodyInserters.fromValue(Map.of(
                        "partitionId", request.getPartitionId(),
                        "message", request.getMessage()
                    )))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                
                responses.add(response);
            } catch (Exception e) {
                // Node failure, continue
            }
        }
        
        if (responses.size() >= 2) { // W=2 quorum
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "replicas", responses.size(),
                "details", responses
            ));
        }
        
        return ResponseEntity.internalServerError()
            .body(Map.of("status", "error", "message", "Quorum not met"));
    }
    
    @GetMapping("/read/{partitionId}/{version}")
    public ResponseEntity<Map<String, Object>> read(
            @PathVariable String partitionId,
            @PathVariable Long version) {
        // Use read repair service
        try {
            Map<String, Object> response = webClient.get()
                .uri(readRepairServiceUrl + "/api/read-repair/read/" + partitionId + "/" + version)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

class WriteRequest {
    private String partitionId;
    private String message;
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
