package com.example.mapreduce.gateway.dto;

import lombok.Data;

@Data
public class JobRequest {
    private String jobName;
    private String inputTopic;
    private int numMappers = 4;
    private int numReducers = 2;
    private String mapFunction = "WORD_COUNT";
    private String reduceFunction = "SUM";
}
