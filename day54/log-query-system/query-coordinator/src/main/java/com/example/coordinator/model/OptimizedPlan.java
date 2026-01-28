package com.example.coordinator.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OptimizedPlan {
    private String selectedIndex;
    private QueryPlan.Predicate primaryPredicate;
    private List<QueryPlan.Predicate> additionalFilters;
    private List<String> projectionFields;
    private boolean useCache;
    private String cacheKey;
    private int estimatedResultSize;
    private long estimatedExecutionTimeMs;
}
