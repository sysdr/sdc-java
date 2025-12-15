package com.logprocessor.hints;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hints", indexes = {
    @Index(name = "idx_target_node", columnList = "targetNodeUrl"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class Hint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String targetNodeUrl;
    
    @Column(nullable = false)
    private String partitionId;
    
    @Column(nullable = false, length = 2000)
    private String payload;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant deliveredAt;
    
    @Column(nullable = false)
    private String status; // PENDING, DELIVERED, EXPIRED
    
    @Column
    private Integer retryCount;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTargetNodeUrl() { return targetNodeUrl; }
    public void setTargetNodeUrl(String targetNodeUrl) { this.targetNodeUrl = targetNodeUrl; }
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}
