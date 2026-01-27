package com.example.alerts.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String timestamp;
    private String service;
    private String level;
    private String message;
    private String traceId;
    private Integer responseTime;
    private Integer statusCode;
}
