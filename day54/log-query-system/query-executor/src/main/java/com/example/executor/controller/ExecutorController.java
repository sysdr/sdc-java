package com.example.executor.controller;

import com.example.executor.service.LocalQueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class ExecutorController {
    
    @Autowired
    private LocalQueryExecutor queryExecutor;
    
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody Map<String, Object> queryPlan) {
        log.info("Received query execution request");
        
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> results = queryExecutor.executeQuery(queryPlan);
        long executionTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("totalRows", results.size());
        response.put("executionTimeMs", executionTime);
        response.put("nodeId", System.getenv("NODE_ID"));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "query-executor");
        health.put("nodeId", System.getenv("NODE_ID"));
        return ResponseEntity.ok(health);
    }
}
