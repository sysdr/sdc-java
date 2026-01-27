package com.example.anomalydetector;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "anomalies", indexes = {
    @Index(name = "idx_service_timestamp", columnList = "serviceName,timestamp"),
    @Index(name = "idx_confidence", columnList = "confidence")
})
@Data
public class AnomalyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String eventId;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Column(nullable = false)
    private Long timestamp;
    
    @Column(nullable = false)
    private Double confidence;
    
    @Column(nullable = false)
    private Integer detectionCount;
    
    @Column(columnDefinition = "TEXT")
    private String rawData;
}
