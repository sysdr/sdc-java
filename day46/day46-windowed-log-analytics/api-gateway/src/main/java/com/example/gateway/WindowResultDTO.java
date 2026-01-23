package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindowResultDTO {
    private String windowKey;
    private long windowStart;
    private long windowEnd;
    private String windowType;
    private long eventCount;
    private long errorCount;
    private long warnCount;
    private double avgLatencyMs;
    private int maxLatencyMs;
    private int minLatencyMs;
    private double p95LatencyMs;
    private double errorRate;
    private long computedAt;
}
