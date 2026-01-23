package com.example.mapreduce.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReduceTask {
    private String taskId;
    private String jobId;
    private int partition;
    private String reduceFunction;
    private TaskStatus status;
    private String workerId;
    private int retryCount;
}
