package com.example.storagenode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    @Query("SELECT e FROM LogEntry e WHERE e.entryKey = :key ORDER BY e.timestamp DESC")
    List<LogEntry> findByEntryKey(String key);
    
    @Query("SELECT DISTINCT e.entryKey FROM LogEntry e")
    List<String> findAllKeys();
}
