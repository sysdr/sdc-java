package com.example.logprocessor.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface LogRepository extends JpaRepository<LogEntity, String> {
    
    List<LogEntity> findBySourceIp(String sourceIp);
    
    List<LogEntity> findBySourceIpAndTimestampBetween(
        String sourceIp, Instant start, Instant end);
    
    List<LogEntity> findByLevel(String level);
    
    @Query("SELECT COUNT(l) FROM LogEntity l")
    long countAllLogs();
}
