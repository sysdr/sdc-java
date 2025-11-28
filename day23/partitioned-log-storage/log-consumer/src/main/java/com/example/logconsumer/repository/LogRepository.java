package com.example.logconsumer.repository;

import com.example.logconsumer.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    
    /**
     * Find logs within time range and optional source filter.
     * PostgreSQL will use partition pruning based on logDate if constraints exist.
     */
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp >= :start AND l.timestamp < :end " +
           "AND (:source IS NULL OR l.source = :source) " +
           "ORDER BY l.timestamp DESC")
    List<LogEntry> findByTimeRangeAndSource(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("source") String source
    );
}
