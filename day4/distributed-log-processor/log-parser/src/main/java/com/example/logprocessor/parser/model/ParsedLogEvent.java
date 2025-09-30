package com.example.logprocessor.parser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public class ParsedLogEvent {
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("http_method")
    private String httpMethod;
    
    @JsonProperty("request_path")
    private String requestPath;
    
    @JsonProperty("status_code")
    private Integer statusCode;
    
    @JsonProperty("response_size")
    private Long responseSize;
    
    @JsonProperty("user_agent")
    private String userAgent;
    
    @JsonProperty("referer")
    private String referer;
    
    @JsonProperty("response_time")
    private Double responseTime;
    
    @JsonProperty("log_format")
    private String logFormat;
    
    @JsonProperty("raw_log")
    private String rawLog;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public ParsedLogEvent() {}
    
    public ParsedLogEvent(String rawLog, String logFormat) {
        this.rawLog = rawLog;
        this.logFormat = logFormat;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
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
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
