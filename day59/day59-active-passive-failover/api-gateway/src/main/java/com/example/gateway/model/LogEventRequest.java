package com.example.gateway.model;

import lombok.Data;

@Data
public class LogEventRequest {
    private String level;
    private String message;
    private String source;
}
