package com.example.loadgen.controller;

import com.example.loadgen.model.LoadTestConfig;
import com.example.loadgen.model.LoadTestResult;
import com.example.loadgen.service.LoadTestOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/loadtest")
public class LoadTestController {
    
    private final LoadTestOrchestrator orchestrator;
    private LoadTestResult latestResult;
    
    public LoadTestController(LoadTestOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    
    @PostMapping("/burst")
    public ResponseEntity<LoadTestResult> executeBurstTest(@RequestBody LoadTestConfig config) {
        log.info("Executing burst test: {}", config.getTestName());
        latestResult = orchestrator.executeBurstTest(config);
        return ResponseEntity.ok(latestResult);
    }
    
    @PostMapping("/ramp")
    public ResponseEntity<LoadTestResult> executeRampTest(@RequestBody LoadTestConfig config) {
        log.info("Executing ramp test: {}", config.getTestName());
        latestResult = orchestrator.executeRampTest(config);
        return ResponseEntity.ok(latestResult);
    }
    
    @GetMapping("/results/latest")
    public ResponseEntity<LoadTestResult> getLatestResult() {
        if (latestResult == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latestResult);
    }
}
