package com.example.readgateway.controller;

import com.example.readgateway.service.ReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/read")
public class ReadController {
    
    private final ReadService readService;
    
    public ReadController(ReadService readService) {
        this.readService = readService;
    }
    
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> read(@PathVariable("key") String key) {
        try {
            Map<String, Object> result = readService.read(key);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Read request failed", e);
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
