package com.example.mapreduce.coordinator.entity;

import com.example.mapreduce.common.model.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {
    @Id
    private String taskId;
    
    private String jobId;
    private String taskType; // MAP or REDUCE
    private int partition;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    private String workerId;
    private int retryCount;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
}
