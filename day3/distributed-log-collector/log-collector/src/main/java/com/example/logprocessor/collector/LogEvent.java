package com.example.logprocessor.collector;

import java.time.LocalDateTime;

public class LogEvent {
    private String id;
    private String filePath;
    private String content;
    private long fileOffset;
    private LocalDateTime timestamp;
    private String hostname;
    private String serviceName;
    private String contentHash;

    public LogEvent() {}

    public LogEvent(String filePath, String content, long fileOffset) {
        this.filePath = filePath;
        this.content = content;
        this.fileOffset = fileOffset;
        this.timestamp = LocalDateTime.now();
        this.hostname = System.getenv().getOrDefault("HOSTNAME", "localhost");
        this.serviceName = "log-collector";
        this.contentHash = String.valueOf(content.hashCode());
        this.id = generateId();
    }

    private String generateId() {
        return String.format("%s-%d-%s", 
                            filePath.hashCode(), 
                            fileOffset, 
                            timestamp.toString());
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getFileOffset() { return fileOffset; }
    public void setFileOffset(long fileOffset) { this.fileOffset = fileOffset; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    @Override
    public String toString() {
        return String.format("LogEvent{id='%s', filePath='%s', content='%s', offset=%d}", 
                           id, filePath, content.substring(0, Math.min(50, content.length())), fileOffset);
    }
}
