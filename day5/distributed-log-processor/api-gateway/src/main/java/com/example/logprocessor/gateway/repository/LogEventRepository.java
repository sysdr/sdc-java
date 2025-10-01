package com.example.logprocessor.gateway.repository;

import com.example.logprocessor.gateway.model.LogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
    
    Page<LogEvent> findByLevel(String level, Pageable pageable);
    
    Page<LogEvent> findBySource(String source, Pageable pageable);
    
    Page<LogEvent> findByLevelAndSource(String level, String source, Pageable pageable);
    
    Page<LogEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEvent> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEvent> findBySourceAndTimestampBetween(String source, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEvent> findByLevelAndSourceAndTimestampBetween(String level, String source, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    LogEvent findByTraceId(String traceId);
    
    Page<LogEvent> findByMessageContainingIgnoreCase(String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(le) FROM LogEvent le WHERE le.level = :level AND le.timestamp > :since")
    long countByLevelAndTimestampAfter(@Param("level") String level, @Param("since") LocalDateTime since);
}
