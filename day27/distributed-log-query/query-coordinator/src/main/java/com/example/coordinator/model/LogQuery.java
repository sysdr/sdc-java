package com.example.coordinator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQuery {
    private Instant startTime;
    private Instant endTime;
    private String logLevel;
    private String serviceName;
    private String messagePattern;
    private Integer limit;
    
    public Integer getLimit() {
        return limit != null ? limit : 1000;
    }
}
