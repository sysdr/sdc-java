package com.example.coordinator.controller;

import com.example.coordinator.model.ClusterTopology;
import com.example.coordinator.service.TopologyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorController {
    
    private final TopologyService topologyService;
    
    public CoordinatorController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }
    
    @GetMapping("/topology")
    public ResponseEntity<ClusterTopology> getTopology() {
        return ResponseEntity.ok(topologyService.getCurrentTopology());
    }
    
    @GetMapping("/nodes/{key}")
    public ResponseEntity<Map<String, Object>> getNodesForKey(
            @PathVariable("key") String key,
            @RequestParam(value = "count", defaultValue = "3") int count) {
        List<String> nodes = topologyService.getNodesForKey(key, count);
        return ResponseEntity.ok(Map.of(
            "key", key,
            "nodes", nodes,
            "count", nodes.size()
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HEALTHY");
    }
}
