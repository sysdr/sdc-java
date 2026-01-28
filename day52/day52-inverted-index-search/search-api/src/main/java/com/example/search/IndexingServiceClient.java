package com.example.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String INDEXING_SERVICE_URL = "http://indexing-service:8082";
    
    @CircuitBreaker(name = "indexing-service", fallbackMethod = "searchFallback")
    public List<Long> search(String query, boolean withScores) {
        try {
            String url = String.format("%s/api/index/search?query=%s&withScores=%s", 
                INDEXING_SERVICE_URL, query, withScores);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode json = objectMapper.readTree(response);
            JsonNode resultsNode = json.get("results");
            if (resultsNode == null || !resultsNode.isArray()) {
                return Collections.emptyList();
            }
            return StreamSupport.stream(resultsNode.spliterator(), false)
                .map(node -> node.get("docId").asLong())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error calling indexing service", e);
            return Collections.emptyList();
        }
    }
    
    private List<Long> searchFallback(String query, boolean withScores, Exception ex) {
        log.warn("Circuit breaker activated for indexing service: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
