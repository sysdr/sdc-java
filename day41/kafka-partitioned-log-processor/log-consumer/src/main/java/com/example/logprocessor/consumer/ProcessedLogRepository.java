package com.example.logprocessor.consumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ProcessedLogRepository extends JpaRepository<ProcessedLog, Long> {
    
    boolean existsByEventId(String eventId);
    
    List<ProcessedLog> findBySource(String source);
    
    @Query("SELECT p.partition as partition, COUNT(p) as count " +
           "FROM ProcessedLog p GROUP BY p.partition ORDER BY p.partition")
    List<Map<String, Object>> getPartitionDistribution();
}
