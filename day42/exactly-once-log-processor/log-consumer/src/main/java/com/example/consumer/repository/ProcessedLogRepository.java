package com.example.consumer.repository;

import com.example.consumer.model.ProcessedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedLogRepository extends JpaRepository<ProcessedLog, Long> {
    
    boolean existsByEventId(String eventId);
    
    long countByPartition(int partition);
    
    @Query("SELECT COUNT(p) FROM ProcessedLog p WHERE p.processedAt > :since")
    long countProcessedSince(java.time.Instant since);
}
