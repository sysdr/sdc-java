package com.example.mapreduce.coordinator.controller;

import com.example.mapreduce.common.model.Job;
import com.example.mapreduce.coordinator.service.CoordinatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coordinator")
@RequiredArgsConstructor
@Slf4j
public class CoordinatorController {
    
    private final CoordinatorService coordinatorService;
    
    @PostMapping("/jobs")
    public ResponseEntity<Job> submitJob(@RequestBody Map<String, Object> request) {
        String jobName = (String) request.get("jobName");
        String inputTopic = (String) request.get("inputTopic");
        int numMappers = ((Number) request.getOrDefault("numMappers", 4)).intValue();
        int numReducers = ((Number) request.getOrDefault("numReducers", 2)).intValue();
        String mapFunction = (String) request.getOrDefault("mapFunction", "WORD_COUNT");
        String reduceFunction = (String) request.getOrDefault("reduceFunction", "SUM");
        
        Job job = coordinatorService.submitJob(jobName, inputTopic, numMappers, numReducers, mapFunction, reduceFunction);
        return ResponseEntity.ok(job);
    }
    
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Job> getJob(@PathVariable String jobId) {
        Job job = coordinatorService.getJob(jobId);
        return job != null ? ResponseEntity.ok(job) : ResponseEntity.notFound().build();
    }
    
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String taskId) {
        coordinatorService.completeTask(taskId);
        return ResponseEntity.ok().build();
    }
}
