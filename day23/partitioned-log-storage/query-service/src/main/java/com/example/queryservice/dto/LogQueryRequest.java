package com.example.queryservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogQueryRequest {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String source;
    private String level;
    private Integer limit = 1000;
}
