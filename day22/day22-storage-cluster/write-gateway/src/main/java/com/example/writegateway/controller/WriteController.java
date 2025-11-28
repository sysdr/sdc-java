package com.example.writegateway.controller;

import com.example.writegateway.model.WriteRequest;
import com.example.writegateway.service.WriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/write")
public class WriteController {
    
    private final WriteService writeService;
    
    public WriteController(WriteService writeService) {
        this.writeService = writeService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> write(@RequestBody WriteRequest request) {
        try {
            Map<String, Object> result = writeService.write(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Write request failed", e);
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HEALTHY");
    }
}
