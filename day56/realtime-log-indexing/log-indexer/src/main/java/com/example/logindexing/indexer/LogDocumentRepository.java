package com.example.logindexing.indexer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogDocumentRepository extends JpaRepository<LogDocument, String> {
    
    List<LogDocument> findByLevel(String level);
    
    List<LogDocument> findByService(String service);
    
    List<LogDocument> findByTimestampBetween(Instant start, Instant end);
    
    @Query("SELECT l FROM LogDocument l WHERE l.timestamp >= :start ORDER BY l.timestamp DESC")
    List<LogDocument> findRecentLogs(Instant start);
}
