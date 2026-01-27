package com.example.dashboard.service;

import com.example.dashboard.model.MetricPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Consumes metrics from Kafka and pushes to WebSocket clients.
 * Implements batching to reduce WebSocket frame overhead.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketMetricsService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    // Buffer for batching metrics
    private final ConcurrentLinkedQueue<MetricPoint> pendingMetrics = new ConcurrentLinkedQueue<>();
    
    @KafkaListener(topics = "metrics-aggregated", groupId = "dashboard-websocket")
    public void consumeMetric(String message) {
        try {
            MetricPoint metric = objectMapper.readValue(message, MetricPoint.class);
            pendingMetrics.offer(metric);
        } catch (Exception e) {
            log.error("Error consuming metric: {}", e.getMessage());
        }
    }
    
    /**
     * Batch and send metrics every 100ms to reduce WebSocket overhead.
     */
    @Scheduled(fixedRate = 100)
    public void flushMetrics() {
        if (pendingMetrics.isEmpty()) {
            return;
        }
        
        List<MetricPoint> batch = new ArrayList<>();
        MetricPoint metric;
        while ((metric = pendingMetrics.poll()) != null && batch.size() < 100) {
            batch.add(metric);
        }
        
        if (!batch.isEmpty()) {
            try {
                messagingTemplate.convertAndSend("/topic/metrics", batch);
            } catch (Exception e) {
                log.error("Error sending metrics to WebSocket: {}", e.getMessage());
            }
        }
    }
}
