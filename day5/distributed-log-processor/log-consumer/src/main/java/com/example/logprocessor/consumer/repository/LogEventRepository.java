package com.example.logprocessor.consumer.repository;

import com.example.logprocessor.consumer.model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
    
    List<LogEvent> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);
    
    List<LogEvent> findBySourceAndTimestampAfter(String source, LocalDateTime timestamp);
    
    LogEvent findByTraceId(String traceId);
    
    @Query("SELECT le FROM LogEvent le WHERE le.message LIKE %:keyword% AND le.timestamp > :since")
    List<LogEvent> findByMessageContainingAndTimestampAfter(@Param("keyword") String keyword, 
                                                           @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(le) FROM LogEvent le WHERE le.level = :level AND le.timestamp > :since")
    long countByLevelAndTimestampAfter(@Param("level") String level, @Param("since") LocalDateTime since);
}
