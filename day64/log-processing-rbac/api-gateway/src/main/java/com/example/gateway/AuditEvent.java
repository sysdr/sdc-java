package com.example.gateway;

import java.time.LocalDateTime;

public class AuditEvent {
    private LocalDateTime timestamp;
    private Long userId;
    private String username;
    private String action;
    private String resource;
    private String result; // GRANTED, DENIED
    private Integer recordsReturned;
    private String query;

    public AuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Integer getRecordsReturned() { return recordsReturned; }
    public void setRecordsReturned(Integer recordsReturned) { this.recordsReturned = recordsReturned; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
