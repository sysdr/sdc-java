package com.example.coordinator.controller;

import com.example.coordinator.model.OptimizedPlan;
import com.example.coordinator.model.QueryPlan;
import com.example.coordinator.service.QueryCoordinatorService;
import com.example.coordinator.service.QueryOptimizer;
import com.example.coordinator.service.QueryParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@Slf4j
public class QueryController {
    
    @Autowired
    private QueryParsingService parsingService;
    
    @Autowired
    private QueryOptimizer optimizer;
    
    @Autowired
    private QueryCoordinatorService coordinator;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        log.info("Received query: {}", query);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse query
            QueryPlan plan = parsingService.parseQuery(query);
            
            // Optimize query
            OptimizedPlan optimizedPlan = optimizer.optimize(plan);
            
            // Execute query
            List<Map<String, Object>> results = coordinator.executeQuery(optimizedPlan);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("totalRows", results.size());
            response.put("executionTimeMs", executionTime);
            response.put("selectedIndex", optimizedPlan.getSelectedIndex());
            response.put("fromCache", optimizedPlan.isUseCache());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Query execution failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "query-coordinator");
        return ResponseEntity.ok(health);
    }
}
