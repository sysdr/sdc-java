package com.example.logprocessor.gateway;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogQueryRepository extends JpaRepository<LogEventEntity, String> {
    
    Page<LogEventEntity> findByTimestampBetween(
            LocalDateTime start, 
            LocalDateTime end, 
            Pageable pageable
    );
    
    Page<LogEventEntity> findByLevelAndTimestampBetween(
            String level, 
            LocalDateTime start, 
            LocalDateTime end, 
            Pageable pageable
    );
    
    Page<LogEventEntity> findBySourceAndTimestampBetween(
            String source, 
            LocalDateTime start, 
            LocalDateTime end, 
            Pageable pageable
    );
    
    @Query("SELECT COUNT(l) FROM LogEventEntity l WHERE l.level = :level")
    long countByLevel(@Param("level") String level);
    
    @Query("SELECT l.source, COUNT(l) FROM LogEventEntity l GROUP BY l.source")
    List<Object[]> countBySource();
    
    @Query("SELECT l.level, COUNT(l) FROM LogEventEntity l WHERE l.timestamp >= :since GROUP BY l.level")
    List<Object[]> getLogLevelStats(@Param("since") LocalDateTime since);
    
    @Query(value = "SELECT * FROM log_events WHERE message ILIKE %:searchTerm% " +
                   "AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC",
           nativeQuery = true)
    Page<LogEventEntity> searchByMessage(
            @Param("searchTerm") String searchTerm,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}
