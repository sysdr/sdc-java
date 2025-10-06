package com.example.logconsumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, String> {
    List<LogEntry> findByLevelOrderByTimestampDesc(String level);
    List<LogEntry> findByServiceOrderByTimestampDesc(String service);
    List<LogEntry> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);
}
