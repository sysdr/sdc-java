package com.example.logproducer;

public class LogEventRequest {
    private String level;
    private String service;
    private String message;
    private boolean shouldFail;
    
    // Getters and Setters
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isShouldFail() { return shouldFail; }
    public void setShouldFail(boolean shouldFail) { this.shouldFail = shouldFail; }
}
