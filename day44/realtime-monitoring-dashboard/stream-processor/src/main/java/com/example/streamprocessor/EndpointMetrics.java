package com.example.streamprocessor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointMetrics {
    private String endpoint;
    private Long windowStart;
    private Long windowEnd;
    private Long totalRequests;
    private Long errorCount;
    private Double errorRate;
    private Double avgResponseTime;
    private Long p50ResponseTime;
    private Long p95ResponseTime;
    private Long p99ResponseTime;
    
    @Builder.Default
    private List<Long> responseTimes = new ArrayList<>();
    
    public void addRequest(LogEvent event) {
        if (totalRequests == null) totalRequests = 0L;
        if (errorCount == null) errorCount = 0L;
        
        totalRequests++;
        if (event.getStatusCode() >= 400) {
            errorCount++;
        }
        responseTimes.add(event.getResponseTimeMs());
    }
    
    public void calculateMetrics() {
        if (totalRequests > 0) {
            errorRate = (double) errorCount / totalRequests * 100;
            
            // Calculate average
            avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            
            // Calculate percentiles
            List<Long> sorted = new ArrayList<>(responseTimes);
            sorted.sort(Long::compareTo);
            
            if (!sorted.isEmpty()) {
                p50ResponseTime = sorted.get((int) (sorted.size() * 0.50));
                p95ResponseTime = sorted.get((int) (sorted.size() * 0.95));
                p99ResponseTime = sorted.get((int) (sorted.size() * 0.99));
            }
        }
    }
}
