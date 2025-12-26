package com.systemdesign.logprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerMetrics {
    private String groupId;
    private int activeConsumers;
    private Map<Integer, Long> partitionLags;
    private long totalProcessed;
    private long totalFailed;
    private double averageProcessingTimeMs;
    private long lastRebalanceTimestamp;
    private int rebalanceCount;
}
