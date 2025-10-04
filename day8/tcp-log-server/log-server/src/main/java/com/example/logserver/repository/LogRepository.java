package com.example.logserver.repository;

import com.example.logserver.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for log entry persistence.
 */
@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {

    /**
     * Find logs by level within time range.
     * Uses index on (level, timestamp) for efficient queries.
     */
    List<LogEntry> findByLevelAndTimestampBetween(
        String level, 
        Instant start, 
        Instant end
    );

    /**
     * Find recent logs by source.
     */
    List<LogEntry> findTop100BySourceOrderByTimestampDesc(String source);

    /**
     * Count logs by level in time range.
     */
    @Query("SELECT l.level, COUNT(l) FROM LogEntry l " +
           "WHERE l.timestamp BETWEEN :start AND :end " +
           "GROUP BY l.level")
    List<Object[]> countByLevelBetween(Instant start, Instant end);
}
