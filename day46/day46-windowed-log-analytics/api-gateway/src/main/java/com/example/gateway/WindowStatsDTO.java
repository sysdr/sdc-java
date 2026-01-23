package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindowStatsDTO {
    private long totalEvents;
    private long totalErrors;
    private double overallErrorRate;
    private double avgLatencyMs;
    private int maxLatencyMs;
    private int windowCount;
}
