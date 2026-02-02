package com.example.gateway;

import lombok.Data;

@Data
public class LogDocument {
    private String timestamp;
    private String service;
    private String level;
    private String message;
    private String traceId;
    private String hostname;
}
