package com.example.aggregator.service;

import com.example.aggregator.model.LogEvent;
import com.example.aggregator.model.MetricPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes raw log events and maintains in-memory buffers of recent metrics.
 * This enables sub-millisecond queries for real-time dashboard updates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsConsumerService {
    
    private final ObjectMapper objectMapper;
    private final MetricsPublisher metricsPublisher;
    private final MetricsCacheService cacheService;
    private final MetricsPersistenceService persistenceService;
    
    // Ring buffers for recent metrics (last 1000 points per metric)
    private final Map<String, CircularFifoQueue<MetricPoint>> metricBuffers = new ConcurrentHashMap<>();
    private static final int BUFFER_SIZE = 1000;
    
    @KafkaListener(topics = "log-events", groupId = "metrics-aggregator")
    public void consumeLogEvent(String message) {
        try {
            LogEvent event = objectMapper.readValue(message, LogEvent.class);
            processLogEvent(event);
        } catch (Exception e) {
            log.error("Error processing log event: {}", e.getMessage());
        }
    }
    
    private void processLogEvent(LogEvent event) {
        Instant now = Instant.now();
        
        // Generate metrics from log event
        if ("ERROR".equals(event.getLevel())) {
            addMetric("error_rate", 1.0, now, "service=" + event.getService());
        }
        
        if (event.getResponseTime() != null) {
            addMetric("response_time", event.getResponseTime().doubleValue(), now, 
                     "service=" + event.getService() + ",endpoint=" + event.getEndpoint());
        }
        
        if (event.getStatusCode() != null && event.getStatusCode() >= 500) {
            addMetric("http_5xx_count", 1.0, now, "service=" + event.getService());
        }
        
        addMetric("request_count", 1.0, now, "service=" + event.getService());
    }
    
    private void addMetric(String metricName, Double value, Instant timestamp, String labels) {
        MetricPoint point = MetricPoint.builder()
            .metricName(metricName)
            .value(value)
            .timestamp(timestamp)
            .labels(labels)
            .build();
        
        // Add to in-memory buffer
        getBuffer(metricName).add(point);
        
        // Publish to WebSocket clients
        metricsPublisher.publishMetric(point);
        
        // Cache in Redis
        cacheService.cacheMetric(point);
        
        // Persist to database (async)
        persistenceService.persistMetric(point);
    }
    
    private CircularFifoQueue<MetricPoint> getBuffer(String metricName) {
        return metricBuffers.computeIfAbsent(metricName, k -> new CircularFifoQueue<>(BUFFER_SIZE));
    }
    
    public CircularFifoQueue<MetricPoint> getMetricBuffer(String metricName) {
        return metricBuffers.get(metricName);
    }
}
