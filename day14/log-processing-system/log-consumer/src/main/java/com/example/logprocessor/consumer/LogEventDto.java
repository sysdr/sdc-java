package com.example.logprocessor.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEventDto {
    private String id;
    private String level;
    private String message;
    private String source;
    private String timestamp;
    private String traceId;
}
