package com.example.analyzer.controller;

import com.example.analyzer.service.MetricsAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    
    private final MetricsAnalysisService analysisService;
    
    public AnalysisController(MetricsAnalysisService analysisService) {
        this.analysisService = analysisService;
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> analyzeMetrics() {
        return ResponseEntity.ok(analysisService.analyzeMetrics());
    }
}
