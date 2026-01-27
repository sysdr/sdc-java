package com.example.anomalydetector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String eventId;
    private String userId;
    private String serviceName;
    private String endpoint;
    private int responseTime;
    private int statusCode;
    private double cpuUsage;
    private double memoryUsage;
    private int errorCount;
    private long timestamp;
    private boolean isAnomaly;
}
