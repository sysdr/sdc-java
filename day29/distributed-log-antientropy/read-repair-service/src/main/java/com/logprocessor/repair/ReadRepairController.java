package com.logprocessor.repair;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/read-repair")
public class ReadRepairController {
    
    @Autowired
    private ReadRepairService readRepairService;
    
    @GetMapping("/read/{partitionId}/{version}")
    public ResponseEntity<Map<String, Object>> readWithRepair(
            @PathVariable String partitionId,
            @PathVariable Long version) {
        Map<String, Object> result = readRepairService.readWithRepair(partitionId, version);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
