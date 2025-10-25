package com.example.logprocessor.service;

import com.example.logprocessor.model.LogStats;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

@Service
public class StatsService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public LogStats calculateStats() {
        // Get counts from critical_logs table
        Query criticalCountQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM critical_logs WHERE priority = 'CRITICAL'");
        Long criticalCount = ((Number) criticalCountQuery.getSingleResult()).longValue();
        
        Query highCountQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM critical_logs WHERE priority = 'HIGH'");
        Long highCount = ((Number) highCountQuery.getSingleResult()).longValue();
        
        // Get counts from normal_logs table
        Query normalCountQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM normal_logs WHERE priority = 'NORMAL'");
        Long normalCount = ((Number) normalCountQuery.getSingleResult()).longValue();
        
        Query lowCountQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM normal_logs WHERE priority = 'LOW'");
        Long lowCount = ((Number) lowCountQuery.getSingleResult()).longValue();
        
        // Calculate average latencies
        Query criticalLatencyQuery = entityManager.createNativeQuery(
            "SELECT AVG(EXTRACT(EPOCH FROM (processed_at - timestamp)) * 1000) " +
            "FROM critical_logs WHERE priority = 'CRITICAL' AND processed_at IS NOT NULL");
        Object criticalLatencyResult = criticalLatencyQuery.getSingleResult();
        Double avgCriticalLatency = (criticalLatencyResult != null) ? 
            ((Number) criticalLatencyResult).doubleValue() : 0.0;
        
        Query normalLatencyQuery = entityManager.createNativeQuery(
            "SELECT AVG(EXTRACT(EPOCH FROM (processed_at - timestamp)) * 1000) " +
            "FROM normal_logs WHERE priority = 'NORMAL' AND processed_at IS NOT NULL");
        Object normalLatencyResult = normalLatencyQuery.getSingleResult();
        Double avgNormalLatency = (normalLatencyResult != null) ? 
            ((Number) normalLatencyResult).doubleValue() : 0.0;
        
        return LogStats.builder()
            .criticalCount(criticalCount)
            .highCount(highCount)
            .normalCount(normalCount)
            .lowCount(lowCount)
            .totalCount(criticalCount + highCount + normalCount + lowCount)
            .avgCriticalLatencyMs(avgCriticalLatency != null ? avgCriticalLatency : 0.0)
            .avgNormalLatencyMs(avgNormalLatency != null ? avgNormalLatency : 0.0)
            .build();
    }
}
