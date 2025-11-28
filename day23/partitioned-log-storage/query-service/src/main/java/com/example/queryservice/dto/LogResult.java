package com.example.queryservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LogResult {
    private Long id;
    private String source;
    private LocalDateTime timestamp;
    private String message;
    private String level;
    private String traceId;
}
