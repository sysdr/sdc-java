package com.example.logprocessor.consumer.repository;

import com.example.logprocessor.consumer.model.LogEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEventEntity, String> {

    @Query("SELECT e FROM LogEventEntity e WHERE e.source = :source AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<LogEventEntity> findBySourceSince(@Param("source") String source, @Param("since") Instant since);

    @Query("SELECT e FROM LogEventEntity e WHERE e.level = :level ORDER BY e.timestamp DESC LIMIT 50")
    List<LogEventEntity> findByLevelRecent(@Param("level") String level);
}
