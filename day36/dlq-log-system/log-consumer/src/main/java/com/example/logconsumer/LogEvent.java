package com.example.logconsumer;

public class LogEvent {
    private String messageId;
    private String level;
    private String service;
    private String message;
    private long timestamp;
    private boolean shouldFail;
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isShouldFail() { return shouldFail; }
    public void setShouldFail(boolean shouldFail) { this.shouldFail = shouldFail; }
}
