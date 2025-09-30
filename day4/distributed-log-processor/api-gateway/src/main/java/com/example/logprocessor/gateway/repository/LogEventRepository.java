package com.example.logprocessor.gateway.repository;

import com.example.logprocessor.gateway.entity.LogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
    
    Page<LogEvent> findByIpAddress(String ipAddress, Pageable pageable);
    
    Page<LogEvent> findByStatusCode(Integer statusCode, Pageable pageable);
    
    Page<LogEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    @Query("SELECT le FROM LogEvent le WHERE le.ipAddress = :ip AND le.timestamp >= :since")
    List<LogEvent> findRecentByIpAddress(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT le.statusCode, COUNT(le) FROM LogEvent le GROUP BY le.statusCode")
    List<Object[]> getStatusCodeCounts();
    
    @Query("SELECT HOUR(le.timestamp) as hour, COUNT(le) as count FROM LogEvent le WHERE le.timestamp >= :since GROUP BY HOUR(le.timestamp) ORDER BY hour")
    List<Object[]> getHourlyRequestCounts(@Param("since") LocalDateTime since);
}
