package com.example.logproducer.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Routes log events to appropriate partitions based on time and source.
 * Implements composite partitioning strategy: daily time windows + source hashing.
 */
@Service
public class PartitionRouterService {
    
    private static final int SOURCE_PARTITION_COUNT = 256;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    
    /**
     * Calculate partition key for Kafka topic distribution.
     * Format: {source}_{date} ensures events from same source on same day go to same partition.
     */
    public String calculateKafkaPartitionKey(String source, LocalDateTime timestamp) {
        String dateStr = timestamp.format(DATE_FORMATTER);
        return String.format("%s_%s", source, dateStr);
    }
    
    /**
     * Calculate source hash for database sub-partitioning.
     * Uses modulo 256 to distribute sources evenly across sub-partitions.
     */
    public int calculateSourceHash(String source) {
        return Math.abs(source.hashCode() % SOURCE_PARTITION_COUNT);
    }
    
    /**
     * Get the target partition name for database writes.
     * Format: logs_YYYY_MM_DD_p{000-255}
     */
    public String getDatabasePartition(LocalDateTime timestamp, String source) {
        String dateStr = timestamp.format(DATE_FORMATTER);
        int sourceHash = calculateSourceHash(source);
        return String.format("logs_%s_p%03d", dateStr, sourceHash);
    }
}
