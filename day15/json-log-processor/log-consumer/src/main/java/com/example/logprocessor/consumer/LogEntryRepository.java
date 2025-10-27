package com.example.logprocessor.consumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    List<LogEntry> findByServiceAndTimestampBetween(
        String service, Instant start, Instant end);
    
    List<LogEntry> findByLevel(String level);
    
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.timestamp > :since")
    long countLogsSince(Instant since);
    
    @Query("SELECT l.service, COUNT(l) FROM LogEntry l GROUP BY l.service")
    List<Object[]> countLogsByService();
}
