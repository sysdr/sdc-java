package com.example.logprocessor.consumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    Page<LogEntry> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);
    
    Page<LogEntry> findByLogLevel(String logLevel, Pageable pageable);
    
    Page<LogEntry> findBySource(String source, Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "AND l.logLevel = :logLevel ORDER BY l.timestamp DESC")
    Page<LogEntry> findByTimestampBetweenAndLogLevel(
        @Param("startTime") Instant startTime, 
        @Param("endTime") Instant endTime, 
        @Param("logLevel") String logLevel, 
        Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:keyword% " +
           "AND l.timestamp BETWEEN :startTime AND :endTime ORDER BY l.timestamp DESC")
    Page<LogEntry> findByMessageContainingAndTimestampBetween(
        @Param("keyword") String keyword,
        @Param("startTime") Instant startTime, 
        @Param("endTime") Instant endTime, 
        Pageable pageable);
    
    @Query("SELECT l.logLevel, COUNT(l) FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY l.logLevel")
    List<Object[]> getLogLevelCounts(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    @Query("SELECT l.source, COUNT(l) FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY l.source ORDER BY COUNT(l) DESC")
    List<Object[]> getTopSourcesByCount(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
