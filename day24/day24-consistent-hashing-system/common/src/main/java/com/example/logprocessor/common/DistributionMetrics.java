package com.example.logprocessor.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionMetrics {
    private int totalNodes;
    private int virtualNodesPerNode;
    private long totalLogs;
    private Map<String, Long> logsPerNode;
    private double standardDeviation;
    private double balanceScore; // 0-100, higher is better
    private String mostLoadedNode;
    private String leastLoadedNode;
}
