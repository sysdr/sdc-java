package com.example.producer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ClusterHealthReporter {
    private static final Logger logger = LoggerFactory.getLogger(ClusterHealthReporter.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${cluster.coordinator.url:http://cluster-coordinator:8081}")
    private String coordinatorUrl;
    
    @Value("${cluster.node.id:producer-1}")
    private String nodeId;
    
    public ClusterHealthReporter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Scheduled(fixedRate = 5000)
    public void reportHealth() {
        try {
            Map<String, Object> healthReport = new HashMap<>();
            healthReport.put("nodeId", nodeId);
            healthReport.put("status", "HEALTHY");
            healthReport.put("timestamp", System.currentTimeMillis());
            
            String url = coordinatorUrl + "/cluster/health";
            restTemplate.postForObject(url, healthReport, Void.class);
            
        } catch (Exception e) {
            logger.warn("Failed to report health to coordinator: {}", e.getMessage());
        }
    }
}
