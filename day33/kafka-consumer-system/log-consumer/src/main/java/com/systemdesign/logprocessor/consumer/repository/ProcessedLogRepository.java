package com.systemdesign.logprocessor.consumer.repository;

import com.systemdesign.logprocessor.consumer.entity.ProcessedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProcessedLogRepository extends JpaRepository<ProcessedLog, Long> {
    
    List<ProcessedLog> findByApplicationNameAndTimestampBetween(
        String applicationName, Instant start, Instant end);
    
    @Query("SELECT COUNT(p) FROM ProcessedLog p WHERE p.processedAt > :since")
    long countProcessedSince(Instant since);
    
    @Query("SELECT p.level, COUNT(p) FROM ProcessedLog p WHERE p.timestamp > :since GROUP BY p.level")
    List<Object[]> getLogLevelDistribution(Instant since);
}
