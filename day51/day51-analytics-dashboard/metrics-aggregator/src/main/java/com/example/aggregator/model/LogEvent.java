package com.example.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String timestamp;
    private String level;
    private String service;
    private String message;
    private Integer responseTime;
    private String endpoint;
    private Integer statusCode;
}
