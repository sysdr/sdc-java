package com.example.logprocessor.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class LoadGeneratorController {
    
    private final LoadGeneratorService loadGeneratorService;

    @PostMapping("/burst")
    public ResponseEntity<Map<String, String>> triggerBurst(
            @RequestParam(defaultValue = "10") int durationSeconds) {
        
        loadGeneratorService.generateBurst(durationSeconds);
        
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "duration", durationSeconds + " seconds",
            "message", "Burst load generation started"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "totalGenerated", loadGeneratorService.getGeneratedCount(),
            "status", "running"
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetStats() {
        loadGeneratorService.resetCount();
        return ResponseEntity.ok(Map.of(
            "status", "reset",
            "message", "Statistics reset successfully"
        ));
    }
}
