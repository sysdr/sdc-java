package com.example.gateway;

public class DLQMessage {
    private String messageId;
    private String payload;
    private String errorType;
    private String errorMessage;
    private int retryCount;
    private long dlqTimestamp;
    private String originalTopic;
    
    // Constructor
    public DLQMessage(String messageId, String payload, String errorType, 
                     String errorMessage, int retryCount, long dlqTimestamp, 
                     String originalTopic) {
        this.messageId = messageId;
        this.payload = payload;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.dlqTimestamp = dlqTimestamp;
        this.originalTopic = originalTopic;
    }
    
    // Getters
    public String getMessageId() { return messageId; }
    public String getPayload() { return payload; }
    public String getErrorType() { return errorType; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public long getDlqTimestamp() { return dlqTimestamp; }
    public String getOriginalTopic() { return originalTopic; }
}
