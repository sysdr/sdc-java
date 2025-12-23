package com.example.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MetricsAnalysisService {
    
    private final RestTemplate restTemplate;
    
    public MetricsAnalysisService() {
        this.restTemplate = new RestTemplate();
    }
    
    public Map<String, Object> analyzeMetrics() {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // Fetch metrics from performance monitor
            Map<String, Object> metrics = restTemplate.getForObject(
                "http://localhost:8080/api/performance/metrics", 
                Map.class
            );
            
            if (metrics != null) {
                analysis.put("componentCount", metrics.size());
                analysis.put("status", "healthy");
            } else {
                analysis.put("status", "no_data");
            }
            
            return analysis;
        } catch (Exception e) {
            log.error("Error analyzing metrics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return error;
        }
    }
}
