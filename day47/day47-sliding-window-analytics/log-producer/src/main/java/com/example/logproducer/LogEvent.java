package com.example.logproducer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    @JsonProperty("service_id")
    private String serviceId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("error_rate")
    private double errorRate;
    
    @JsonProperty("latency_ms")
    private double latencyMs;
    
    @JsonProperty("throughput")
    private long throughput;
    
    @JsonProperty("cpu_usage")
    private double cpuUsage;
    
    @JsonProperty("memory_usage")
    private double memoryUsage;
}
