package com.logprocessor.coordinator;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_jobs")
public class ReconciliationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String partitionId;
    
    @Column(nullable = false)
    private String node1Url;
    
    @Column(nullable = false)
    private String node2Url;
    
    @Column(nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED
    
    @Column
    private Integer inconsistenciesFound;
    
    @Column
    private Integer inconsistenciesRepaired;
    
    @Column(nullable = false)
    private Instant scheduledAt;
    
    @Column
    private Instant startedAt;
    
    @Column
    private Instant completedAt;
    
    @Column
    private Integer priority;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
    
    public String getNode1Url() { return node1Url; }
    public void setNode1Url(String node1Url) { this.node1Url = node1Url; }
    
    public String getNode2Url() { return node2Url; }
    public void setNode2Url(String node2Url) { this.node2Url = node2Url; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getInconsistenciesFound() { return inconsistenciesFound; }
    public void setInconsistenciesFound(Integer inconsistenciesFound) { 
        this.inconsistenciesFound = inconsistenciesFound; 
    }
    
    public Integer getInconsistenciesRepaired() { return inconsistenciesRepaired; }
    public void setInconsistenciesRepaired(Integer inconsistenciesRepaired) { 
        this.inconsistenciesRepaired = inconsistenciesRepaired; 
    }
    
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
