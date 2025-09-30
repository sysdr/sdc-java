package com.example.logprocessor.gateway.controller;

import com.example.logprocessor.gateway.entity.LogEvent;
import com.example.logprocessor.gateway.service.LogEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogEventController {
    
    @Autowired
    private LogEventService logEventService;
    
    @GetMapping
    public ResponseEntity<Page<LogEvent>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LogEvent> logs = logEventService.getLogEvents(pageable);
        
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<Page<LogEvent>> getLogsByIp(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEvent> logs = logEventService.getLogEventsByIp(ipAddress, pageable);
        
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/status/{statusCode}")
    public ResponseEntity<Page<LogEvent>> getLogsByStatus(
            @PathVariable Integer statusCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEvent> logs = logEventService.getLogEventsByStatus(statusCode, pageable);
        
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/stats/status-codes")
    public ResponseEntity<List<Map<String, Object>>> getStatusCodeStats() {
        List<Map<String, Object>> stats = logEventService.getStatusCodeStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats/hourly")
    public ResponseEntity<List<Map<String, Object>>> getHourlyStats() {
        List<Map<String, Object>> stats = logEventService.getHourlyStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "api-gateway"));
    }
}
