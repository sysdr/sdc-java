package com.example.logconsumer;

import jakarta.persistence.*;

@Entity
@Table(name = "log_events")
public class LogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "message_id", unique = true)
    private String messageId;
    
    @Column(name = "level")
    private String level;
    
    @Column(name = "service")
    private String service;
    
    @Column(name = "message", length = 4000)
    private String message;
    
    @Column(name = "timestamp")
    private Long timestamp;
    
    @Column(name = "processed_at")
    private Long processedAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }
}
