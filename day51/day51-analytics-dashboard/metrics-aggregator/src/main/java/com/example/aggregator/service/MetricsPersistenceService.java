package com.example.aggregator.service;

import com.example.aggregator.model.AggregatedMetric;
import com.example.aggregator.model.MetricPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Persists metrics to PostgreSQL with time-series optimizations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsPersistenceService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Async
    public void persistMetric(MetricPoint point) {
        try {
            Instant bucketTime = Instant.ofEpochSecond(point.getTimestamp().getEpochSecond() / 60 * 60);
            
            jdbcTemplate.update(
                "INSERT INTO aggregated_metrics (metric_name, value, timestamp, labels, bucket_time) " +
                "VALUES (?, ?, ?::timestamp, ?, ?::timestamp)",
                point.getMetricName(),
                point.getValue(),
                java.sql.Timestamp.from(point.getTimestamp()),
                point.getLabels(),
                java.sql.Timestamp.from(bucketTime)
            );
        } catch (Exception e) {
            log.error("Error persisting metric: {}", e.getMessage());
        }
    }
}
