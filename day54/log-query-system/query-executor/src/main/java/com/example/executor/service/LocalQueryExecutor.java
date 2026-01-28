package com.example.executor.service;

import com.example.executor.model.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalQueryExecutor {
    
    @Autowired
    private EntityManager entityManager;
    
    public List<Map<String, Object>> executeQuery(Map<String, Object> queryPlan) {
        log.info("Executing local query: {}", queryPlan);
        
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<LogEntry> query = cb.createQuery(LogEntry.class);
            Root<LogEntry> root = query.from(LogEntry.class);
            
            List<Predicate> predicates = new ArrayList<>();
            
            // Apply filters from query plan
            if (queryPlan.containsKey("filters")) {
                List<Map<String, Object>> filters = (List<Map<String, Object>>) queryPlan.get("filters");
                for (Map<String, Object> filter : filters) {
                    String field = (String) filter.get("field");
                    String operator = (String) filter.get("operator");
                    Object value = filter.get("value");
                    
                    predicates.add(buildPredicate(cb, root, field, operator, value));
                }
            }
            
            if (!predicates.isEmpty()) {
                query.where(cb.and(predicates.toArray(new Predicate[0])));
            }
            
            // Execute query
            List<LogEntry> results = entityManager.createQuery(query)
                .setMaxResults(1000)
                .getResultList();
            
            // Project fields
            return results.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Query execution failed", e);
            return Collections.emptyList();
        }
    }
    
    private Predicate buildPredicate(CriteriaBuilder cb, Root<LogEntry> root, 
                                     String field, String operator, Object value) {
        Expression<?> path = root.get(field);
        
        return switch (operator) {
            case "=" -> cb.equal(path, value);
            case "!=" -> cb.notEqual(path, value);
            case ">" -> {
                @SuppressWarnings("unchecked")
                Expression<Comparable> comparablePath = (Expression<Comparable>) path;
                yield cb.greaterThan(comparablePath, (Comparable) value);
            }
            case "<" -> {
                @SuppressWarnings("unchecked")
                Expression<Comparable> comparablePath = (Expression<Comparable>) path;
                yield cb.lessThan(comparablePath, (Comparable) value);
            }
            case ">=" -> {
                @SuppressWarnings("unchecked")
                Expression<Comparable> comparablePath = (Expression<Comparable>) path;
                yield cb.greaterThanOrEqualTo(comparablePath, (Comparable) value);
            }
            case "<=" -> {
                @SuppressWarnings("unchecked")
                Expression<Comparable> comparablePath = (Expression<Comparable>) path;
                yield cb.lessThanOrEqualTo(comparablePath, (Comparable) value);
            }
            case "LIKE" -> {
                @SuppressWarnings("unchecked")
                Expression<String> stringPath = (Expression<String>) path;
                yield cb.like(stringPath, "%" + value + "%");
            }
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }
    
    private Map<String, Object> toMap(LogEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("timestamp", entry.getTimestamp().toEpochMilli());
        map.put("level", entry.getLevel());
        map.put("service", entry.getService());
        map.put("message", entry.getMessage());
        map.put("traceId", entry.getTraceId());
        return map;
    }
}
