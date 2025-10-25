package com.example.logprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogStats {
    private long criticalCount;
    private long highCount;
    private long normalCount;
    private long lowCount;
    private long totalCount;
    private double avgCriticalLatencyMs;
    private double avgNormalLatencyMs;
}
