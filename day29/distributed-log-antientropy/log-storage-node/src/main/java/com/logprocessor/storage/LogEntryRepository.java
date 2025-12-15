package com.logprocessor.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    Optional<LogEntry> findByPartitionIdAndVersion(String partitionId, Long version);
    
    List<LogEntry> findByPartitionIdOrderByVersionDesc(String partitionId);
    
    @Query("SELECT e FROM LogEntry e WHERE e.partitionId = :partitionId AND e.version >= :startVersion AND e.version <= :endVersion ORDER BY e.version")
    List<LogEntry> findByPartitionIdAndVersionRange(String partitionId, Long startVersion, Long endVersion);
    
    @Query("SELECT MAX(e.version) FROM LogEntry e WHERE e.partitionId = :partitionId")
    Optional<Long> findMaxVersionByPartitionId(String partitionId);
    
    @Query("SELECT MAX(e.lamportClock) FROM LogEntry e WHERE e.nodeId = :nodeId")
    Optional<Long> findMaxLamportClockByNodeId(String nodeId);
}
