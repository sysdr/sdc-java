package com.example.mapreduce.gateway.dto;

import com.example.mapreduce.common.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobResponse {
    private String jobId;
    private JobStatus status;
    private String message;
}
