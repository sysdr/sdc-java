package com.example.logproducer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private boolean isAnomaly; // For synthetic anomaly injection
    
    public LogEvent(String eventId, String userId, String serviceName, 
                   String endpoint, int responseTime, int statusCode,
                   double cpuUsage, double memoryUsage, int errorCount) {
        this.eventId = eventId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.responseTime = responseTime;
        this.statusCode = statusCode;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.errorCount = errorCount;
        this.timestamp = Instant.now().toEpochMilli();
        this.isAnomaly = false;
    }
}
