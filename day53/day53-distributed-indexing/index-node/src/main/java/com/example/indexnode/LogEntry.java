package com.example.indexnode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    private String logId;
    private String tenantId;
    private Long timestamp;
    private String level;
    private String message;
    private String service;
}
