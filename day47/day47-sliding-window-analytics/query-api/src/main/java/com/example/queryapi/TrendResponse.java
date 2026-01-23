package com.example.queryapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponse {
    @JsonProperty("service_id")
    private String serviceId;
    
    @JsonProperty("one_min_avg_latency")
    private double oneMinAvgLatency;
    
    @JsonProperty("five_min_avg_latency")
    private double fiveMinAvgLatency;
    
    @JsonProperty("fifteen_min_avg_latency")
    private double fifteenMinAvgLatency;
    
    @JsonProperty("one_min_avg_error_rate")
    private double oneMinAvgErrorRate;
    
    @JsonProperty("five_min_avg_error_rate")
    private double fiveMinAvgErrorRate;
    
    @JsonProperty("fifteen_min_avg_error_rate")
    private double fifteenMinAvgErrorRate;
    
    @JsonProperty("one_min_throughput")
    private double oneMinThroughput;
    
    @JsonProperty("five_min_throughput")
    private double fiveMinThroughput;
    
    @JsonProperty("fifteen_min_throughput")
    private double fifteenMinThroughput;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("from_cache")
    private boolean fromCache;
    
    // Explicit setters/getters for Lombok compatibility
    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
    
    public boolean isFromCache() {
        return fromCache;
    }
}
