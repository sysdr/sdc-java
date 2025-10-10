package com.example.logshipper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
