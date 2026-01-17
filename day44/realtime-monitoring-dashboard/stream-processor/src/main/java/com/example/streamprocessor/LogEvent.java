package com.example.streamprocessor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
