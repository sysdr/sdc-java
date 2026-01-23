package com.example.queryapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowStats {
    @JsonProperty("count")
    private long count = 0;
    
    @JsonProperty("sum_error_rate")
    private double sumErrorRate = 0.0;
    
    @JsonProperty("sum_latency")
    private double sumLatency = 0.0;
    
    @JsonProperty("sum_throughput")
    private long sumThroughput = 0;
    
    @JsonProperty("sum_cpu")
    private double sumCpu = 0.0;
    
    @JsonProperty("sum_memory")
    private double sumMemory = 0.0;
    
    @JsonProperty("max_latency")
    private double maxLatency = 0.0;
    
    @JsonProperty("min_latency")
    private double minLatency = Double.MAX_VALUE;
    
    public WindowStats update(LogEvent event) {
        this.count++;
        this.sumErrorRate += event.getErrorRate();
        this.sumLatency += event.getLatencyMs();
        this.sumThroughput += event.getThroughput();
        this.sumCpu += event.getCpuUsage();
        this.sumMemory += event.getMemoryUsage();
        this.maxLatency = Math.max(this.maxLatency, event.getLatencyMs());
        this.minLatency = Math.min(this.minLatency, event.getLatencyMs());
        return this;
    }
    
    public double getAvgErrorRate() {
        return count > 0 ? sumErrorRate / count : 0.0;
    }
    
    public double getAvgLatency() {
        return count > 0 ? sumLatency / count : 0.0;
    }
    
    public double getAvgThroughput() {
        return count > 0 ? (double) sumThroughput / count : 0.0;
    }
    
    public double getAvgCpu() {
        return count > 0 ? sumCpu / count : 0.0;
    }
    
    public double getAvgMemory() {
        return count > 0 ? sumMemory / count : 0.0;
    }
    
    // Explicit getter for Lombok compatibility
    public long getCount() {
        return count;
    }
}
