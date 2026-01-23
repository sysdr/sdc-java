package com.example.processor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WindowResultRepository extends JpaRepository<WindowResultEntity, Long> {
    
    @Query("SELECT w FROM WindowResultEntity w WHERE w.windowKey = :key " +
           "AND w.windowType = :type AND w.windowStart >= :start AND w.windowEnd <= :end " +
           "ORDER BY w.windowStart DESC")
    List<WindowResultEntity> findByKeyAndTypeAndTimeRange(
        @Param("key") String key,
        @Param("type") String type,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    @Query("SELECT w FROM WindowResultEntity w WHERE w.windowStart >= :start " +
           "ORDER BY w.windowStart DESC")
    List<WindowResultEntity> findRecent(@Param("start") Instant start);
}
