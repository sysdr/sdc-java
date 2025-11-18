package com.example.logprocessor.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.producer.url}")
    private String producerUrl;

    @Value("${services.consumer.url}")
    private String consumerUrl;

    @Value("${services.schema-registry.url:http://localhost:8085}")
    private String schemaRegistryUrl;

    @GetMapping("/schema/subjects")
    public ResponseEntity<?> getSchemaSubjects() {
        return restTemplate.getForEntity(
            schemaRegistryUrl + "/subjects",
            Object.class
        );
    }

    @GetMapping("/schema/subjects/{subject}/versions")
    public ResponseEntity<?> getSchemaVersions(@PathVariable String subject) {
        try {
            return restTemplate.getForEntity(
                schemaRegistryUrl + "/subjects/" + subject + "/versions",
                Object.class
            );
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Subject not found or error occurred");
            error.put("message", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @PostMapping("/logs")
    public ResponseEntity<?> forwardLogEvent(@RequestBody Map<String, Object> request) {
        return restTemplate.postForEntity(
            producerUrl + "/api/logs",
            request,
            Map.class
        );
    }

    @PostMapping("/logs/batch")
    public ResponseEntity<?> forwardBatchLogEvents(@RequestBody java.util.List<?> requests) {
        return restTemplate.postForEntity(
            producerUrl + "/api/logs/batch",
            requests,
            Map.class
        );
    }

    @GetMapping("/logs/{eventId}")
    public ResponseEntity<?> getLogEvent(@PathVariable String eventId) {
        return restTemplate.getForEntity(
            consumerUrl + "/api/consumer/logs/" + eventId,
            Map.class
        );
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<?> getByCorrelation(@PathVariable String correlationId) {
        return restTemplate.getForEntity(
            consumerUrl + "/api/consumer/correlation/" + correlationId,
            Map.class
        );
    }

    @GetMapping("/consumer/status")
    public ResponseEntity<?> getConsumerStatus() {
        return restTemplate.getForEntity(
            consumerUrl + "/api/consumer/status",
            Map.class
        );
    }

    @GetMapping("/metrics/producer")
    public ResponseEntity<?> getProducerMetrics() {
        try {
            String metrics = restTemplate.getForObject(
                producerUrl + "/actuator/prometheus",
                String.class
            );
            return ResponseEntity.ok(parsePrometheusMetrics(metrics));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch producer metrics");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/metrics/consumer")
    public ResponseEntity<?> getConsumerMetrics() {
        try {
            String metrics = restTemplate.getForObject(
                consumerUrl + "/actuator/prometheus",
                String.class
            );
            return ResponseEntity.ok(parsePrometheusMetrics(metrics));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch consumer metrics");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private Map<String, Double> parsePrometheusMetrics(String metrics) {
        Map<String, Double> result = new HashMap<>();
        if (metrics == null) return result;
        
        String[] lines = metrics.split("\n");
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            
            // Look for avro metrics - check for exact metric names
            String metricName = null;
            if (line.startsWith("avro_events_produced_total")) {
                metricName = "avro_events_produced_total";
            } else if (line.startsWith("avro_events_consumed_total")) {
                metricName = "avro_events_consumed_total";
            } else if (line.startsWith("avro_events_v1_total")) {
                metricName = "avro_events_v1_total";
            } else if (line.startsWith("avro_events_v2_total")) {
                metricName = "avro_events_v2_total";
            } else if (line.startsWith("avro_events_errors_total")) {
                metricName = "avro_events_errors_total";
            } else if (line.startsWith("avro_events_processing_errors_total")) {
                metricName = "avro_events_processing_errors_total";
            }
            
            if (metricName != null) {
                // Extract value - format: metric_name{tags} value
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        // Get the last part which should be the value
                        String valueStr = parts[parts.length - 1].trim();
                        double value = Double.parseDouble(valueStr);
                        result.put(metricName, value);
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                }
            }
        }
        return result;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("gateway", "healthy");
        
        try {
            restTemplate.getForEntity(producerUrl + "/actuator/health", Map.class);
            health.put("producer", "healthy");
        } catch (Exception e) {
            health.put("producer", "unhealthy: " + e.getMessage());
        }
        
        try {
            restTemplate.getForEntity(consumerUrl + "/actuator/health", Map.class);
            health.put("consumer", "healthy");
        } catch (Exception e) {
            health.put("consumer", "unhealthy: " + e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}
