package com.example.logprocessor.gateway.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_events")
public class LogEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "request_path", length = 500)
    private String requestPath;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "response_size")
    private Long responseSize;
    
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
    @Column(name = "referer", length = 500)
    private String referer;
    
    @Column(name = "response_time")
    private Double responseTime;
    
    @Column(name = "log_format", length = 50)
    private String logFormat;
    
    @Column(name = "raw_log", columnDefinition = "TEXT")
    private String rawLog;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public LogEvent() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
    
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    
    public Long getResponseSize() { return responseSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    
    public Double getResponseTime() { return responseTime; }
    public void setResponseTime(Double responseTime) { this.responseTime = responseTime; }
    
    public String getLogFormat() { return logFormat; }
    public void setLogFormat(String logFormat) { this.logFormat = logFormat; }
    
    public String getRawLog() { return rawLog; }
    public void setRawLog(String rawLog) { this.rawLog = rawLog; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
