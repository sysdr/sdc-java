package com.example.query.repository;

import com.example.query.entity.StoredLogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<StoredLogEvent, String> {
    
    List<StoredLogEvent> findByEventType(String eventType);
    
    List<StoredLogEvent> findByTimestampBetween(Instant start, Instant end);
    
    @Query("SELECT e FROM StoredLogEvent e WHERE e.severity = ?1 ORDER BY e.timestamp DESC")
    List<StoredLogEvent> findBySeverityOrderByTimestampDesc(String severity);
}
