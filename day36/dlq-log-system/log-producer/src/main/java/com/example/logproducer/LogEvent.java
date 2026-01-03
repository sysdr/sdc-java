package com.example.logproducer;

public class LogEvent {
    private String messageId;
    private String level;
    private String service;
    private String message;
    private long timestamp;
    private boolean shouldFail;
    
    private LogEvent(Builder builder) {
        this.messageId = builder.messageId;
        this.level = builder.level;
        this.service = builder.service;
        this.message = builder.message;
        this.timestamp = builder.timestamp;
        this.shouldFail = builder.shouldFail;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getMessageId() { return messageId; }
    public String getLevel() { return level; }
    public String getService() { return service; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isShouldFail() { return shouldFail; }
    
    public static class Builder {
        private String messageId;
        private String level;
        private String service;
        private String message;
        private long timestamp;
        private boolean shouldFail;
        
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }
        
        public Builder level(String level) {
            this.level = level;
            return this;
        }
        
        public Builder service(String service) {
            this.service = service;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder shouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
            return this;
        }
        
        public LogEvent build() {
            return new LogEvent(this);
        }
    }
}
