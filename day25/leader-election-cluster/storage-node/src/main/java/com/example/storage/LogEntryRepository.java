package com.example.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByTermAndLogIndexGreaterThan(Long term, Long logIndex);
    
    @Query("SELECT MAX(e.logIndex) FROM LogEntry e")
    Long findMaxLogIndex();
    
    @Query("SELECT e FROM LogEntry e WHERE e.logIndex >= :startIndex ORDER BY e.logIndex")
    List<LogEntry> findEntriesFromIndex(Long startIndex);
    
    long countByCommittedTrue();
}
