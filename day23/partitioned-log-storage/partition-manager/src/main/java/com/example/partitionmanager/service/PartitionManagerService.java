package com.example.partitionmanager.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages partition lifecycle: creation, maintenance, and archival.
 * Runs hourly to ensure partitions exist before needed (pre-provisioning).
 */
@Service
public class PartitionManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PartitionManagerService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final int SOURCE_PARTITIONS = 256;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${partition.retention.days:90}")
    private int retentionDays;
    
    private final Counter createdPartitionsCounter;
    private final Counter archivedPartitionsCounter;
    
    public PartitionManagerService(MeterRegistry meterRegistry) {
        this.createdPartitionsCounter = Counter.builder("partitions.created.total")
                .description("Total partitions created")
                .register(meterRegistry);
        this.archivedPartitionsCounter = Counter.builder("partitions.archived.total")
                .description("Total partitions archived")
                .register(meterRegistry);
    }
    
    @PostConstruct
    public void initialize() {
        createParentTable();
        ensurePartitionsExist();
    }
    
    /**
     * Create parent partitioned table if not exists.
     */
    private void createParentTable() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS logs (
                id BIGSERIAL,
                source VARCHAR(255) NOT NULL,
                source_hash INTEGER NOT NULL,
                log_date TIMESTAMP NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                message TEXT,
                level VARCHAR(50) NOT NULL,
                trace_id VARCHAR(255),
                partition_key VARCHAR(255),
                PRIMARY KEY (id, log_date, source_hash)
            ) PARTITION BY RANGE (log_date)
            """;
        
        jdbcTemplate.execute(createTable);
        logger.info("Parent logs table created or already exists");
    }
    
    /**
     * Run hourly to ensure partitions exist.
     * Creates partitions 24 hours in advance to prevent write failures.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void ensurePartitionsExist() {
        logger.info("Starting partition maintenance");
        
        // Create partitions for today, tomorrow, and day after
        for (int i = 0; i <= 2; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            createDailyPartition(date);
        }
        
        // Archive old partitions
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        archiveOldPartitions(cutoffDate);
        
        logger.info("Partition maintenance completed");
    }
    
    /**
     * Create a daily partition with 256 source sub-partitions.
     */
    private void createDailyPartition(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        String parentTableName = "logs_" + dateStr;
        
        try {
            // Check if parent partition exists
            String checkQuery = "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = ?)";
            Boolean exists = jdbcTemplate.queryForObject(checkQuery, Boolean.class, parentTableName);
            
            if (Boolean.TRUE.equals(exists)) {
                logger.debug("Partition {} already exists", parentTableName);
                return;
            }
            
            // Create parent daily partition
            String createParent = String.format("""
                CREATE TABLE %s PARTITION OF logs
                FOR VALUES FROM ('%s') TO ('%s')
                PARTITION BY HASH (source_hash)
                """,
                parentTableName,
                date,
                date.plusDays(1)
            );
            
            jdbcTemplate.execute(createParent);
            logger.info("Created parent partition: {}", parentTableName);
            
            // Create 256 source hash sub-partitions
            for (int i = 0; i < SOURCE_PARTITIONS; i++) {
                String subPartitionName = String.format("%s_p%03d", parentTableName, i);
                
                String createSub = String.format("""
                    CREATE TABLE %s PARTITION OF %s
                    FOR VALUES WITH (MODULUS %d, REMAINDER %d)
                    """,
                    subPartitionName,
                    parentTableName,
                    SOURCE_PARTITIONS,
                    i
                );
                
                jdbcTemplate.execute(createSub);
                
                // Create indexes on sub-partition
                createPartitionIndexes(subPartitionName);
            }
            
            createdPartitionsCounter.increment();
            logger.info("Created {} sub-partitions for {}", SOURCE_PARTITIONS, parentTableName);
            
        } catch (Exception e) {
            logger.error("Failed to create partition for date: {}", date, e);
        }
    }
    
    /**
     * Create performance indexes on each sub-partition.
     */
    private void createPartitionIndexes(String tableName) {
        try {
            // Index on timestamp for time-range queries
            String timestampIdx = String.format(
                "CREATE INDEX IF NOT EXISTS %s_timestamp_idx ON %s (timestamp DESC)",
                tableName, tableName
            );
            jdbcTemplate.execute(timestampIdx);
            
            // Index on source for source-specific queries
            String sourceIdx = String.format(
                "CREATE INDEX IF NOT EXISTS %s_source_idx ON %s (source)",
                tableName, tableName
            );
            jdbcTemplate.execute(sourceIdx);
            
            // Composite index for common query pattern
            String compositeIdx = String.format(
                "CREATE INDEX IF NOT EXISTS %s_source_level_idx ON %s (source, level, timestamp DESC)",
                tableName, tableName
            );
            jdbcTemplate.execute(compositeIdx);
            
        } catch (Exception e) {
            logger.warn("Failed to create indexes for {}", tableName, e);
        }
    }
    
    /**
     * Archive partitions older than retention period.
     * Uses DETACH instead of DROP to allow manual recovery if needed.
     */
    private void archiveOldPartitions(LocalDate cutoffDate) {
        try {
            // Find all partitions older than cutoff
            String findOldPartitions = """
                SELECT tablename FROM pg_tables 
                WHERE schemaname = 'public' 
                AND tablename LIKE 'logs_____________________________'
                """;
            
            List<String> allPartitions = jdbcTemplate.queryForList(findOldPartitions, String.class);
            
            for (String partition : allPartitions) {
                try {
                    // Extract date from partition name (logs_YYYY_MM_DD_p000)
                    String datePart = partition.substring(5, 15);
                    LocalDate partitionDate = LocalDate.parse(datePart, DATE_FORMATTER);
                    
                    if (partitionDate.isBefore(cutoffDate)) {
                        // Detach partition (makes it a regular table)
                        String parentName = partition.substring(0, 15);  // logs_YYYY_MM_DD
                        String detach = String.format(
                            "ALTER TABLE %s DETACH PARTITION %s",
                            parentName, partition
                        );
                        jdbcTemplate.execute(detach);
                        
                        archivedPartitionsCounter.increment();
                        logger.info("Detached old partition: {}", partition);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process partition: {}", partition, e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to archive old partitions", e);
        }
    }
}
