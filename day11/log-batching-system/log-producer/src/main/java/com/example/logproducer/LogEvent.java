package com.example.logproducer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String id;
    private String level;
    private String service;
    private String message;
    private String timestamp;
    private String traceId;
    
    public static LogEvent create(String level, String service, String message) {
        LogEvent event = new LogEvent();
        event.setId(java.util.UUID.randomUUID().toString());
        event.setLevel(level);
        event.setService(service);
        event.setMessage(message);
        event.setTimestamp(Instant.now().toString());
        event.setTraceId(java.util.UUID.randomUUID().toString().substring(0, 8));
        return event;
    }
}
