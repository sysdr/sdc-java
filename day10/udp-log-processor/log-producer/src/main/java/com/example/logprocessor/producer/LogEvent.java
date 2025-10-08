package com.example.logprocessor.producer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String id;
    private String source;
    private String level;
    private String message;
    private Instant timestamp;
    private Long sequenceNumber;
}
