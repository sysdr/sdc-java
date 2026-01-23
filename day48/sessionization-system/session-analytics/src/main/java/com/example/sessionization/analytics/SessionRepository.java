package com.example.sessionization.analytics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
    List<SessionEntity> findByUserIdOrderByStartTimeDesc(String userId);
    
    Page<SessionEntity> findByHasConversion(boolean hasConversion, Pageable pageable);
    
    @Query("SELECT s FROM SessionEntity s WHERE s.startTime >= :startTime")
    List<SessionEntity> findRecentSessions(Instant startTime);
    
    @Query("SELECT AVG(s.durationSeconds) FROM SessionEntity s WHERE s.startTime >= :startTime")
    Double getAverageDuration(Instant startTime);
    
    @Query("SELECT AVG(s.eventCount) FROM SessionEntity s WHERE s.startTime >= :startTime")
    Double getAverageEventCount(Instant startTime);
    
    @Query("SELECT COUNT(s) FROM SessionEntity s WHERE s.hasConversion = true AND s.startTime >= :startTime")
    Long getConversionCount(Instant startTime);
}
