package com.example.apigateway;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "anomalies")
@Data
public class AnomalyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String eventId;
    private String serviceName;
    private Long timestamp;
    private Double confidence;
    private Integer detectionCount;
    
    @Column(columnDefinition = "TEXT")
    private String rawData;
}
