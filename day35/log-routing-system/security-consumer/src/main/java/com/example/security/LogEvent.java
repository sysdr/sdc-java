package com.example.security;

import lombok.Data;
import java.util.Map;

@Data
public class LogEvent {
    private String id;
    private String timestamp;
    private String severity;
    private String source;
    private String type;
    private String message;
    private Map<String, Object> metadata;
}
