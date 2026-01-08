package com.example.logconsumer.repository;

import com.example.logconsumer.model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, String> {
    
    List<LogEvent> findByServiceAndTimestampBetween(
        String service, Instant start, Instant end);
    
    @Query("SELECT COUNT(l) FROM LogEvent l WHERE l.level = 'ERROR' AND l.timestamp > :since")
    Long countErrorsSince(Instant since);
    
    List<LogEvent> findTop100ByLevelOrderByTimestampDesc(String level);
}
