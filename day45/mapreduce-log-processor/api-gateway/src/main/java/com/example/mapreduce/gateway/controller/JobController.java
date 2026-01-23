package com.example.mapreduce.gateway.controller;

import com.example.mapreduce.common.model.Job;
import com.example.mapreduce.gateway.dto.JobRequest;
import com.example.mapreduce.gateway.dto.JobResponse;
import com.example.mapreduce.gateway.service.CoordinatorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    
    private final CoordinatorClient coordinatorClient;
    
    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@RequestBody JobRequest request) {
        try {
            Job job = coordinatorClient.submitJob(request);
            return ResponseEntity.ok(new JobResponse(
                job.getJobId(),
                job.getStatus(),
                "Job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit job", e);
            return ResponseEntity.internalServerError()
                .body(new JobResponse(null, null, "Job submission failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{jobId}")
    public ResponseEntity<Job> getJob(@PathVariable String jobId) {
        try {
            Job job = coordinatorClient.getJob(jobId);
            return job != null ? ResponseEntity.ok(job) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get job", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{jobId}/results")
    public ResponseEntity<?> getResults(@PathVariable String jobId) {
        // Placeholder - would query PostgreSQL results table
        return ResponseEntity.ok("Results endpoint - query results table in PostgreSQL");
    }
}
