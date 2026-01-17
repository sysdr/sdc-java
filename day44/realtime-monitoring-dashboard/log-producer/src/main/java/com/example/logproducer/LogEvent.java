package com.example.logproducer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private String eventId;
    private Long timestamp;
    private String endpoint;
    private String method;
    private Integer statusCode;
    private Long responseTimeMs;
    private String userId;
    private String region;
    private String service;
}
