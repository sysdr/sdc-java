package com.example.logconsumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, String> {
    
    List<LogEntry> findByServiceOrderByTimestampDesc(String service);
    
    List<LogEntry> findByLevelOrderByTimestampDesc(String level);
    
    @Query("SELECT l FROM LogEntry l ORDER BY l.timestamp DESC LIMIT 100")
    List<LogEntry> findRecent();
}
