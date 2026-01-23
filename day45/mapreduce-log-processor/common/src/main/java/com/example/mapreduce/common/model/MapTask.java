package com.example.mapreduce.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapTask {
    private String taskId;
    private String jobId;
    private int partition;
    private long startOffset;
    private long endOffset;
    private int numReducers;
    private String mapFunction;
    private TaskStatus status;
    private String workerId;
    private int retryCount;
}
