package com.example.logprocessor.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchNormalizationResponse {
    private List<NormalizationResult> results;
    private BatchStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchStats {
        private int total;
        private int successful;
        private int failed;
        private long totalProcessingTimeMs;
        private Map<String, Integer> formatDistribution;
    }
}
