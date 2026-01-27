package com.example.aggregator.service;

import com.example.aggregator.model.MetricPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes aggregated metrics to Kafka topic for dashboard consumption.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String METRICS_TOPIC = "metrics-aggregated";
    
    public void publishMetric(MetricPoint metric) {
        try {
            String message = objectMapper.writeValueAsString(metric);
            kafkaTemplate.send(METRICS_TOPIC, metric.getMetricName(), message);
        } catch (Exception e) {
            log.error("Error publishing metric: {}", e.getMessage());
        }
    }
}
