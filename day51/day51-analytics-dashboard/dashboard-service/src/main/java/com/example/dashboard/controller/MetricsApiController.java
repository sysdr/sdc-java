package com.example.dashboard.controller;

import com.example.dashboard.model.MetricPoint;
import com.example.dashboard.service.DashboardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MetricsApiController {
    
    private final DashboardQueryService queryService;
    
    @GetMapping("/{metricName}")
    public List<MetricPoint> getMetrics(
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        
        return queryService.queryMetrics(metricName, start, end);
    }
    
    @GetMapping("/health")
    public String health() {
        return "Dashboard API is healthy";
    }
}
