package com.example.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {
    
    private final LeaderDiscoveryService leaderDiscovery;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @PostMapping("/write")
    @CircuitBreaker(name = "storageCluster", fallbackMethod = "writeFallback")
    public ResponseEntity<Map<String, Object>> write(@RequestBody Map<String, String> payload) {
        String leader = leaderDiscovery.discoverLeader();
        
        if (leader == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "No leader available"
            ));
        }
        
        try {
            String url = String.format("%s/write", leader);
            WriteRequest request = new WriteRequest(
                payload.get("data"),
                payload.getOrDefault("source", "gateway")
            );
            
            ResponseEntity<WriteResponse> response = restTemplate.postForEntity(
                url, request, WriteResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WriteResponse body = response.getBody();
                return ResponseEntity.ok(Map.of(
                    "success", body.success(),
                    "entryId", body.entryId() != null ? body.entryId() : 0L,
                    "leader", body.leaderId() != null ? body.leaderId() : leader
                ));
            } else if (response.getStatusCode().value() == 307) {
                // Redirect to correct leader
                WriteResponse body = response.getBody();
                if (body != null && body.leaderId() != null) {
                    leaderDiscovery.updateLeader(body.leaderId());
                    return write(payload); // Retry with new leader
                }
            }
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Write failed"
            ));
        } catch (Exception e) {
            log.error("Write failed", e);
            throw e;
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String leader = leaderDiscovery.discoverLeader();
        List<String> nodes = leaderDiscovery.getClusterNodes();
        
        return ResponseEntity.ok(Map.of(
            "leader", leader != null ? leader : "none",
            "clusterSize", nodes.size(),
            "nodes", nodes
        ));
    }
    
    @GetMapping("/nodes/status")
    public ResponseEntity<?> getNodeStatus(@RequestParam(value = "nodeId", required = true) String nodeId) {
        try {
            List<String> nodes = leaderDiscovery.getClusterNodes();
            
            // Find the node URL by nodeId
            String nodeUrl = null;
            
            // Try direct match first
            for (String url : nodes) {
                if (url.contains(nodeId)) {
                    nodeUrl = url;
                    break;
                }
            }
            
            // Try matching storage-node-X pattern if not found (nodeId might be just "1", "2", "3")
            if (nodeUrl == null) {
                // Try as number first
                if (nodeId.matches("\\d+")) {
                    String searchPattern = "storage-node-" + nodeId;
                    for (String url : nodes) {
                        if (url.contains(searchPattern)) {
                            nodeUrl = url;
                            break;
                        }
                    }
                }
                // Try with "node" prefix
                if (nodeUrl == null) {
                    String searchPattern = nodeId.startsWith("storage-node-") ? nodeId : "storage-node-" + nodeId.replace("node", "");
                    for (String url : nodes) {
                        if (url.contains(searchPattern)) {
                            nodeUrl = url;
                            break;
                        }
                    }
                }
            }
            
            if (nodeUrl == null) {
                log.warn("Node not found: {}", nodeId);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Node not found",
                    "nodeId", nodeId
                ));
            }
            
            String url = String.format("%s/raft/status", nodeUrl);
            log.debug("Fetching status from: {}", url);
            
            // Use ParameterizedTypeReference for proper type handling
            ParameterizedTypeReference<Map<String, Object>> responseType = 
                new ParameterizedTypeReference<Map<String, Object>>() {};
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                null, 
                responseType
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }
            
            return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                "error", "Failed to get node status"
            ));
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            log.warn("HTTP error getting status for node {}: {}", nodeId, e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "error", "Node unreachable",
                "nodeId", nodeId,
                "state", "UNREACHABLE",
                "message", e.getMessage() != null ? e.getMessage() : "HTTP error"
            ));
        } catch (Exception e) {
            log.error("Failed to get status for node {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(503).body(Map.of(
                "error", "Node unreachable",
                "nodeId", nodeId,
                "state", "UNREACHABLE",
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }
    
    private ResponseEntity<Map<String, Object>> writeFallback(Map<String, String> payload, Exception e) {
        log.error("Write fallback triggered", e);
        return ResponseEntity.status(503).body(Map.of(
            "success", false,
            "message", "Service temporarily unavailable"
        ));
    }
}

record WriteRequest(String data, String source) {}
record WriteResponse(boolean success, Long entryId, String leaderId, String message) {}
