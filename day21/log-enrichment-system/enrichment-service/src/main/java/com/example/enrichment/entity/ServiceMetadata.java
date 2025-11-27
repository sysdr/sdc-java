package com.example.enrichment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "service_metadata")
@Data
public class ServiceMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String serviceName;
    
    private String version;
    
    @Column(name = "deployment_id")
    private String deploymentId;
    
    private String team;
    
    @Column(name = "cost_center")
    private String costCenter;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
