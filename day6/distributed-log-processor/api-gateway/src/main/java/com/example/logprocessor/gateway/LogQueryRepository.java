package com.example.logprocessor.gateway;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogQueryRepository extends JpaRepository<LogEntry, Long> {
    
    Page<LogEntry> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);
    
    Page<LogEntry> findByLogLevel(String logLevel, Pageable pageable);
    
    Page<LogEntry> findBySource(String source, Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "AND (:logLevel IS NULL OR l.logLevel = :logLevel) " +
           "AND (:source IS NULL OR l.source = :source) " +
           "AND (:keyword IS NULL OR l.message LIKE %:keyword%) " +
           "ORDER BY l.timestamp DESC")
    Page<LogEntry> findWithFilters(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("logLevel") String logLevel,
        @Param("source") String source,
        @Param("keyword") String keyword,
        Pageable pageable);
    
    @Query("SELECT l.logLevel, COUNT(l) FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY l.logLevel")
    List<Object[]> getLogLevelCounts(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    @Query("SELECT l.source, COUNT(l) FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY l.source ORDER BY COUNT(l) DESC")
    List<Object[]> getTopSourcesByCount(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime")
    Long getTotalLogCount(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
