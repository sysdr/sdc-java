package com.example.dashboard;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final RestTemplate restTemplate;
    private final String streamProcessorUrl = "http://stream-processor:8082/api/metrics";

    public DashboardController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/metrics/endpoint/{endpoint}")
    public Map<String, Object> getEndpointMetrics(@PathVariable String endpoint) {
        try {
            return restTemplate.getForObject(
                streamProcessorUrl + "/endpoint/" + endpoint,
                Map.class
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
