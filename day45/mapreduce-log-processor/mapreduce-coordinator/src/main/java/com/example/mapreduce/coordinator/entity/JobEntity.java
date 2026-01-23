package com.example.mapreduce.coordinator.entity;

import com.example.mapreduce.common.model.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {
    @Id
    private String jobId;
    
    private String jobName;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    private String inputTopic;
    private int numMappers;
    private int numReducers;
    
    @Column(columnDefinition = "TEXT")
    private String mapFunction;
    
    @Column(columnDefinition = "TEXT")
    private String reduceFunction;
    
    private Instant createdAt;
    private Instant completedAt;
    
    private long totalMapTasks;
    private long completedMapTasks;
    private long totalReduceTasks;
    private long completedReduceTasks;
}
