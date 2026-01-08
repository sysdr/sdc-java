package com.example.logconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterMessage {
    private String originalMessageId;
    private String topic;
    private Integer partition;
    private Long offset;
    private String failureReason;
    private String exceptionClass;
    private Integer retryCount;
    private Instant timestamp;
    private LogEvent originalPayload;
}
