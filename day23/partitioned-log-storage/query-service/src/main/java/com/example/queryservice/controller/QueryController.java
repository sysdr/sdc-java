package com.example.queryservice.controller;

import com.example.queryservice.dto.LogQueryRequest;
import com.example.queryservice.dto.LogResult;
import com.example.queryservice.service.PartitionAwareQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    
    @Autowired
    private PartitionAwareQueryService queryService;
    
    @PostMapping("/logs")
    public ResponseEntity<List<LogResult>> queryLogs(@RequestBody LogQueryRequest request) {
        List<LogResult> results = queryService.query(request);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
