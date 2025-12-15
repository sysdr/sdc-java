package com.logprocessor.coordinator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorController {
    
    @Autowired
    private ReconciliationJobRepository jobRepository;
    
    @Autowired
    private CoordinatorService coordinatorService;
    
    @GetMapping("/jobs")
    public ResponseEntity<List<ReconciliationJob>> getJobs(@RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(jobRepository.findByStatusOrderByPriorityDesc(status));
        }
        return ResponseEntity.ok(jobRepository.findAll());
    }
    
    @GetMapping("/jobs/{id}")
    public ResponseEntity<ReconciliationJob> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerReconciliation() {
        coordinatorService.scheduleReconciliation();
        return ResponseEntity.ok(Map.of("status", "triggered"));
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
