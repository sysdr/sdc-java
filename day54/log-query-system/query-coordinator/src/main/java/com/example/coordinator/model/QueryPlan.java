package com.example.coordinator.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QueryPlan {
    private SelectClause selectClause;
    private WhereClause whereClause;
    private GroupByClause groupByClause;
    private OrderByClause orderByClause;
    private LimitClause limitClause;
    private String originalQuery;
    
    @Data
    @Builder
    public static class SelectClause {
        private boolean selectAll;
        private List<String> fields;
    }
    
    @Data
    @Builder
    public static class WhereClause {
        private List<Predicate> predicates;
    }
    
    @Data
    @Builder
    public static class Predicate {
        private String field;
        private String operator;
        private Object value;
        private double estimatedSelectivity;
    }
    
    @Data
    @Builder
    public static class GroupByClause {
        private List<String> fields;
    }
    
    @Data
    @Builder
    public static class OrderByClause {
        private List<String> fields;
        private boolean ascending;
    }
    
    @Data
    @Builder
    public static class LimitClause {
        private int limit;
    }
}
