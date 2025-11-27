package com.example.enrichment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "host_metadata")
@Data
public class HostMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String hostname;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    private String datacenter;
    private String environment;
    
    @Column(name = "cost_center")
    private String costCenter;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
