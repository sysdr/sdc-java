package com.example.logprocessor.producer;

import lombok.Data;

@Data
public class LogEventRequest {
    private String source;
    private String level;
    private String message;
    private String application;
    private String hostname;
}
