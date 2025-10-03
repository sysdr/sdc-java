package com.example.logprocessor.gateway;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record LogQueryRequest(
    @NotNull(message = "Start time is required")
    Instant startTime,
    
    @NotNull(message = "End time is required")
    Instant endTime,
    
    String logLevel,
    String source,
    String keyword,
    
    @Min(value = 0, message = "Page must be non-negative")
    int page,
    
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 1000, message = "Size cannot exceed 1000")
    int size
) {
    public LogQueryRequest {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 1000) size = 1000;
    }
}
