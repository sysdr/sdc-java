package com.example.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindowStats {
    private long eventCount;
    private long errorCount;
    private long warnCount;
    private long totalLatency;
    private int maxLatency;
    private int minLatency;
    
    @Builder.Default
    private List<Integer> latencies = new ArrayList<>();
    
    public static WindowStats aggregate(WindowStats stats, String logLevel, int latency) {
        if (stats == null) {
            stats = WindowStats.builder()
                .eventCount(0)
                .errorCount(0)
                .warnCount(0)
                .totalLatency(0)
                .maxLatency(Integer.MIN_VALUE)
                .minLatency(Integer.MAX_VALUE)
                .latencies(new ArrayList<>())
                .build();
        }
        
        stats.eventCount++;
        stats.totalLatency += latency;
        stats.maxLatency = Math.max(stats.maxLatency, latency);
        stats.minLatency = Math.min(stats.minLatency, latency);
        
        if ("ERROR".equals(logLevel)) {
            stats.errorCount++;
        } else if ("WARN".equals(logLevel)) {
            stats.warnCount++;
        }
        
        // Store latencies for percentile calculation (limit size to prevent memory issues)
        if (stats.latencies.size() < 10000) {
            stats.latencies.add(latency);
        }
        
        return stats;
    }
    
    public double getAvgLatency() {
        return eventCount > 0 ? (double) totalLatency / eventCount : 0.0;
    }
    
    public double getErrorRate() {
        return eventCount > 0 ? (double) errorCount / eventCount : 0.0;
    }
    
    public double getP95Latency() {
        if (latencies.isEmpty()) return 0.0;
        
        List<Integer> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        
        int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }
}
