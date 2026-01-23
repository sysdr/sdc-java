package com.example.mapreduce.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private String jobId;
    private String jobName;
    private JobStatus status;
    private String inputTopic;
    private int numMappers;
    private int numReducers;
    private String mapFunction;
    private String reduceFunction;
    private Instant createdAt;
    private Instant completedAt;
    private long totalMapTasks;
    private long completedMapTasks;
    private long totalReduceTasks;
    private long completedReduceTasks;
}
