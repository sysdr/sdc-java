package com.example.partition.repository;

import com.example.partition.entity.LogEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntryEntity, String> {
    
    List<LogEntryEntity> findByTimestampBetweenOrderByTimestampDesc(
        Instant start, Instant end);
    
    List<LogEntryEntity> findByLogLevelAndTimestampBetweenOrderByTimestampDesc(
        String logLevel, Instant start, Instant end);
    
    List<LogEntryEntity> findByServiceNameAndTimestampBetweenOrderByTimestampDesc(
        String serviceName, Instant start, Instant end);
    
    @Query("SELECT l FROM LogEntryEntity l WHERE " +
           "(:logLevel IS NULL OR l.logLevel = :logLevel) AND " +
           "(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
           "l.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY l.timestamp DESC")
    List<LogEntryEntity> findByFilters(
        @Param("logLevel") String logLevel,
        @Param("serviceName") String serviceName,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
    
    @Query("SELECT MIN(l.timestamp) FROM LogEntryEntity l")
    Instant findMinTimestamp();
    
    @Query("SELECT MAX(l.timestamp) FROM LogEntryEntity l")
    Instant findMaxTimestamp();
    
    @Query("SELECT DISTINCT l.logLevel FROM LogEntryEntity l")
    List<String> findDistinctLogLevels();
    
    @Query("SELECT DISTINCT l.serviceName FROM LogEntryEntity l")
    List<String> findDistinctServiceNames();
}
