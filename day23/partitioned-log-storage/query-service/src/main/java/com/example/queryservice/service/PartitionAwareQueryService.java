package com.example.queryservice.service;

import com.example.queryservice.dto.LogQueryRequest;
import com.example.queryservice.dto.LogResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes partition-pruned queries for optimal performance.
 * Analyzes query predicates to determine minimal partition set to scan.
 */
@Service
public class PartitionAwareQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(PartitionAwareQueryService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final Timer queryTimer;
    
    public PartitionAwareQueryService(MeterRegistry meterRegistry) {
        this.queryTimer = Timer.builder("query.duration")
                .description("Query execution time")
                .register(meterRegistry);
    }
    
    /**
     * Execute partition-pruned query.
     * Determines which partitions to scan based on time range and source filter.
     */
    public List<LogResult> query(LogQueryRequest request) {
        return queryTimer.record(() -> {
            List<String> partitions = determinePartitions(request);
            logger.info("Query will scan {} partitions", partitions.size());
            
            if (partitions.isEmpty()) {
                return List.of();
            }
            
            String sql = buildQuery(partitions, request);
            List<Object> params = buildParams(request);
            
            return jdbcTemplate.query(sql, 
                (rs, rowNum) -> LogResult.builder()
                    .id(rs.getLong("id"))
                    .source(rs.getString("source"))
                    .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
                    .message(rs.getString("message"))
                    .level(rs.getString("level"))
                    .traceId(rs.getString("trace_id"))
                    .build(),
                params.toArray()
            );
        });
    }
    
    /**
     * Determine which partitions to scan based on query filters.
     * This is where partition pruning happens.
     */
    private List<String> determinePartitions(LogQueryRequest request) {
        List<String> partitions = new ArrayList<>();
        
        LocalDate startDate = request.getStartTime().toLocalDate();
        LocalDate endDate = request.getEndTime().toLocalDate();
        
        // Iterate through each day in range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateStr = current.format(DATE_FORMATTER);
            String parentPartition = "logs_" + dateStr;
            
            if (request.getSource() != null) {
                // Prune to single source partition
                int sourceHash = Math.abs(request.getSource().hashCode() % 256);
                partitions.add(String.format("%s_p%03d", parentPartition, sourceHash));
            } else {
                // Must scan all source partitions for this day
                for (int i = 0; i < 256; i++) {
                    partitions.add(String.format("%s_p%03d", parentPartition, i));
                }
            }
            
            current = current.plusDays(1);
        }
        
        // Filter to only existing partitions
        return partitions.stream()
            .filter(this::partitionExists)
            .toList();
    }
    
    private boolean partitionExists(String partitionName) {
        try {
            String sql = "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = ?)";
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, partitionName);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Build UNION query across determined partitions.
     * Explicit UNION gives us control over query plan.
     */
    private String buildQuery(List<String> partitions, LogQueryRequest request) {
        StringBuilder sql = new StringBuilder("SELECT * FROM (");
        
        for (int i = 0; i < partitions.size(); i++) {
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT * FROM ").append(partitions.get(i));
        }
        
        sql.append(") AS combined WHERE timestamp >= ? AND timestamp < ?");
        
        if (request.getSource() != null) {
            sql.append(" AND source = ?");
        }
        
        if (request.getLevel() != null) {
            sql.append(" AND level = ?");
        }
        
        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        
        return sql.toString();
    }
    
    private List<Object> buildParams(LogQueryRequest request) {
        List<Object> params = new ArrayList<>();
        params.add(request.getStartTime());
        params.add(request.getEndTime());
        
        if (request.getSource() != null) {
            params.add(request.getSource());
        }
        
        if (request.getLevel() != null) {
            params.add(request.getLevel());
        }
        
        params.add(request.getLimit());
        
        return params;
    }
}
