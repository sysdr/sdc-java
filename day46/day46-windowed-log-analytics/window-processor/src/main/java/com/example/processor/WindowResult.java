package com.example.processor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindowResult {
    @JsonProperty("window_key")
    private String windowKey; // service-name
    
    @JsonProperty("window_start")
    private long windowStart;
    
    @JsonProperty("window_end")
    private long windowEnd;
    
    @JsonProperty("window_type")
    private String windowType; // TUMBLING, HOPPING, SESSION
    
    @JsonProperty("event_count")
    private long eventCount;
    
    @JsonProperty("error_count")
    private long errorCount;
    
    @JsonProperty("warn_count")
    private long warnCount;
    
    @JsonProperty("avg_latency_ms")
    private double avgLatencyMs;
    
    @JsonProperty("max_latency_ms")
    private int maxLatencyMs;
    
    @JsonProperty("min_latency_ms")
    private int minLatencyMs;
    
    @JsonProperty("p95_latency_ms")
    private double p95LatencyMs;
    
    @JsonProperty("error_rate")
    private double errorRate;
    
    @JsonProperty("computed_at")
    private long computedAt;
}
