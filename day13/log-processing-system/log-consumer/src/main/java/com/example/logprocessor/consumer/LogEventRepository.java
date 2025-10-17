package com.example.logprocessor.consumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEventEntity, String> {
    
    List<LogEventEntity> findByLevelOrderByTimestampDesc(String level);
    
    List<LogEventEntity> findBySourceOrderByTimestampDesc(String source);
    
    @Query("SELECT l FROM LogEventEntity l WHERE l.timestamp >= :start ORDER BY l.timestamp DESC")
    List<LogEventEntity> findRecentLogs(Instant start);
    
    long countByLevel(String level);
}
