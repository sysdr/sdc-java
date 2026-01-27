package com.example.aggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "aggregated_metrics", indexes = {
    @Index(name = "idx_metric_time", columnList = "metricName,timestamp"),
    @Index(name = "idx_time", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String metricName;
    private Double value;
    private Instant timestamp;
    private String labels;
    
    @Column(name = "bucket_time")
    private Instant bucketTime;
}
