package com.example.coordinator.service;

import com.example.coordinator.model.QueryPlan;
import com.example.coordinator.model.OptimizedPlan;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryOptimizer {
    
    // Index metadata (in production, this would come from metadata service)
    private static final Map<String, Double> INDEX_SELECTIVITY = Map.of(
        "level", 0.1,        // 10% selectivity for log level
        "service", 0.05,     // 5% selectivity for service name
        "timestamp", 0.2,    // 20% selectivity for time ranges
        "message", 0.5       // 50% selectivity for text search
    );
    
    public OptimizedPlan optimize(QueryPlan plan) {
        log.info("Optimizing query plan: {}", plan);
        
        List<QueryPlan.Predicate> predicates = plan.getWhereClause() != null 
            ? plan.getWhereClause().getPredicates() 
            : Collections.emptyList();
        
        // Estimate selectivity for each predicate
        predicates.forEach(p -> {
            double selectivity = INDEX_SELECTIVITY.getOrDefault(p.getField(), 0.8);
            p.setEstimatedSelectivity(selectivity);
        });
        
        // Sort predicates by selectivity (most selective first)
        List<QueryPlan.Predicate> sortedPredicates = predicates.stream()
            .sorted(Comparator.comparingDouble(QueryPlan.Predicate::getEstimatedSelectivity))
            .collect(Collectors.toList());
        
        // Select primary index
        String selectedIndex = sortedPredicates.isEmpty() 
            ? "full_scan" 
            : "idx_" + sortedPredicates.get(0).getField();
        
        QueryPlan.Predicate primaryPredicate = sortedPredicates.isEmpty() 
            ? null 
            : sortedPredicates.get(0);
        
        List<QueryPlan.Predicate> additionalFilters = sortedPredicates.size() > 1
            ? sortedPredicates.subList(1, sortedPredicates.size())
            : Collections.emptyList();
        
        // Determine projection fields
        List<String> projectionFields = plan.getSelectClause().isSelectAll()
            ? Collections.singletonList("*")
            : plan.getSelectClause().getFields();
        
        // Cache key generation
        String cacheKey = generateCacheKey(plan);
        boolean useCache = shouldUseCache(plan);
        
        // Estimate result size and execution time
        int estimatedResultSize = estimateResultSize(predicates);
        long estimatedExecutionTimeMs = estimateExecutionTime(estimatedResultSize);
        
        OptimizedPlan optimized = OptimizedPlan.builder()
            .selectedIndex(selectedIndex)
            .primaryPredicate(primaryPredicate)
            .additionalFilters(additionalFilters)
            .projectionFields(projectionFields)
            .useCache(useCache)
            .cacheKey(cacheKey)
            .estimatedResultSize(estimatedResultSize)
            .estimatedExecutionTimeMs(estimatedExecutionTimeMs)
            .build();
        
        log.info("Optimized plan: index={}, estimatedSize={}, estimatedTime={}ms", 
            selectedIndex, estimatedResultSize, estimatedExecutionTimeMs);
        
        return optimized;
    }
    
    private String generateCacheKey(QueryPlan plan) {
        return "query:" + Objects.hash(
            plan.getSelectClause(),
            plan.getWhereClause(),
            plan.getGroupByClause(),
            plan.getOrderByClause()
        );
    }
    
    private boolean shouldUseCache(QueryPlan plan) {
        // Cache if no time-based predicates or time range is historical
        return plan.getWhereClause() == null || 
               plan.getWhereClause().getPredicates().stream()
                   .noneMatch(p -> p.getField().equals("timestamp"));
    }
    
    private int estimateResultSize(List<QueryPlan.Predicate> predicates) {
        if (predicates.isEmpty()) return 100000; // Full scan estimate
        
        double combinedSelectivity = predicates.stream()
            .mapToDouble(QueryPlan.Predicate::getEstimatedSelectivity)
            .reduce(1.0, (a, b) -> a * b);
        
        return (int) (100000 * combinedSelectivity);
    }
    
    private long estimateExecutionTime(int resultSize) {
        // Simple linear model: 10μs per result + 50ms base overhead
        return 50 + (resultSize / 100);
    }
}
