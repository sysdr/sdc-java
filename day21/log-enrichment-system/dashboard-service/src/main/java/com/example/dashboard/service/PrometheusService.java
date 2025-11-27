package com.example.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PrometheusService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String PROMETHEUS_URL = "http://localhost:9090";
    
    public PrometheusService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public Map<String, Object> queryMetric(String query) {
        try {
            String url = PROMETHEUS_URL + "/api/v1/query?query=" + java.net.URLEncoder.encode(query, "UTF-8");
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            Map<String, Object> result = new HashMap<>();
            if (jsonNode.has("data") && jsonNode.get("data").has("result")) {
                JsonNode results = jsonNode.get("data").get("result");
                if (results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    if (firstResult.has("value")) {
                        JsonNode value = firstResult.get("value");
                        if (value.isArray() && value.size() == 2) {
                            result.put("timestamp", value.get(0).asText());
                            result.put("value", value.get(1).asText());
                            return result;
                        }
                    }
                }
            }
            
            // Fallback: query service directly
            return queryMetricDirectly(query);
        } catch (Exception e) {
            // Fallback: query service directly
            return queryMetricDirectly(query);
        }
    }
    
    private Map<String, Object> queryMetricDirectly(String query) {
        try {
            // Try to get metrics directly from enrichment service
            String metricsUrl = "http://localhost:8081/actuator/prometheus";
            String response = restTemplate.getForObject(metricsUrl, String.class);
            
            Map<String, Object> result = new HashMap<>();
            result.put("value", "0");
            
            if (response != null) {
                // Parse Prometheus format
                String[] lines = response.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    // Handle different query types
                    if (query.contains("enrichment_attempts_total") || query.contains("enrichment.attempts")) {
                        if (line.startsWith("enrichment_attempts_total")) {
                            String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                            result.put("value", value);
                            return result;
                        }
                    } else if (query.contains("enrichment_successes_total") || query.contains("enrichment.successes")) {
                        if (line.startsWith("enrichment_successes_total")) {
                            String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                            result.put("value", value);
                            return result;
                        }
                    } else if (query.contains("enrichment_coverage") || query.contains("enrichment.coverage")) {
                        if (line.startsWith("enrichment_coverage")) {
                            String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                            try {
                                double coverage = Double.parseDouble(value);
                                result.put("value", String.valueOf(coverage * 100));
                            } catch (NumberFormatException e) {
                                result.put("value", "0");
                            }
                            return result;
                        }
                    } else if (query.contains("enrichment_latency") || query.contains("enrichment.latency")) {
                        // Latency is a timer, look for summary or histogram
                        if (line.contains("enrichment_latency_seconds") && (line.contains("sum") || line.contains("count"))) {
                            String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                            result.put("value", value);
                            return result;
                        }
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("value", "0");
            return result;
        }
    }
    
    public Map<String, String> getDirectMetrics() {
        Map<String, String> metrics = new HashMap<>();
        try {
            String metricsUrl = "http://localhost:8081/actuator/prometheus";
            String response = restTemplate.getForObject(metricsUrl, String.class);
            
            if (response != null) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    if (line.startsWith("enrichment_attempts_total")) {
                        String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                        metrics.put("attempts", value);
                    } else if (line.startsWith("enrichment_successes_total")) {
                        String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                        metrics.put("successes", value);
                    } else if (line.startsWith("enrichment_coverage")) {
                        String value = line.substring(line.lastIndexOf(" ") + 1).trim();
                        metrics.put("coverage", value);
                    }
                }
            }
        } catch (Exception e) {
            // Return empty map
        }
        return metrics;
    }
    
    public List<Map<String, Object>> queryRange(String query, long start, long end, int step) {
        try {
            String url = String.format("%s/api/v1/query_range?query=%s&start=%d&end=%d&step=%d",
                PROMETHEUS_URL,
                java.net.URLEncoder.encode(query, "UTF-8"),
                start, end, step);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            List<Map<String, Object>> timeSeries = new ArrayList<>();
            if (jsonNode.has("data") && jsonNode.get("data").has("result")) {
                JsonNode results = jsonNode.get("data").get("result");
                if (results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    if (firstResult.has("values")) {
                        JsonNode values = firstResult.get("values");
                        for (JsonNode value : values) {
                            if (value.isArray() && value.size() == 2) {
                                Map<String, Object> point = new HashMap<>();
                                point.put("timestamp", value.get(0).asLong());
                                point.put("value", Double.parseDouble(value.get(1).asText()));
                                timeSeries.add(point);
                            }
                        }
                    }
                }
            }
            return timeSeries;
        } catch (Exception e) {
            return Collections.singletonList(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    public Map<String, Object> getServiceHealth(String serviceName, int port) {
        try {
            String url = "http://localhost:" + port + "/actuator/health";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", jsonNode.has("status") ? jsonNode.get("status").asText() : "UNKNOWN");
            health.put("service", serviceName);
            return health;
        } catch (Exception e) {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("service", serviceName);
            health.put("error", e.getMessage());
            return health;
        }
    }
}

