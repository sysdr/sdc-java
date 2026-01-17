package com.example.dashboard;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MetricsAggregator {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final String streamProcessorUrl = "http://stream-processor:8082/api/metrics";

    public MetricsAggregator(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 2000) // Every 2 seconds
    public void aggregateAndBroadcastMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // Fetch current request counts
            Map<String, Long> requestCounts = restTemplate.getForObject(
                streamProcessorUrl + "/requests/current",
                Map.class
            );
            metrics.put("requestCounts", requestCounts);

            // Fetch error counts
            Map<String, Long> errors = restTemplate.getForObject(
                streamProcessorUrl + "/errors",
                Map.class
            );
            metrics.put("errors", errors);

            // Fetch regional metrics
            Map<String, Long> regions = restTemplate.getForObject(
                streamProcessorUrl + "/regions",
                Map.class
            );
            metrics.put("regions", regions);

            metrics.put("timestamp", System.currentTimeMillis());

            // Broadcast to all connected WebSocket clients
            messagingTemplate.convertAndSend("/topic/metrics", metrics);

        } catch (Exception e) {
            // Log error but continue
            System.err.println("Error fetching metrics: " + e.getMessage());
        }
    }
}
